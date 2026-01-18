package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.RedisOrderDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.domain.order.type.OrderType;
import com.kanghwang.khholdings.global.util.IdFormatter;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRedisService {

	private final RedissonClient redissonClient;
	private final OrderRepository orderRepository;
	private final RedisTemplate<String, String> redisTemplate;

	public void processOrder(String orderId, OrderRequestDTO orderDto) {
		// 1. 특정 토큰에 대한 분산 락 획득 (동시 매칭 방지)
		String lockKey = "lock:matching:" + orderDto.getTokenId();
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 최대 5초 대기, 10초간 잠금 점유
			if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				try {
					executeMatching(orderId, orderDto);
				} finally {
					lock.unlock();
				}
			}
		} catch (InterruptedException e) {
			log.error("Matching lock error: {}", e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	// 매칭 엔진
	private void executeMatching(String myOrderId, OrderRequestDTO myOrderDto) {
		OrderSide counterSide = (myOrderDto.getOrderSide() == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;
		String counterOrderBookKey = RedisKeyManager.getOrderBookKey(myOrderDto.getTokenId(), counterSide);
		String orderInfoKey = RedisKeyManager.getOrderInfoKey(myOrderDto.getTokenId());

		RScoredSortedSet<String> counterOrderBook = redissonClient.getScoredSortedSet(counterOrderBookKey); // 상대방의 호가창 ('가격':'주문번호')
		RMap<String, RedisOrderDTO> infoMap = redissonClient.getMap(orderInfoKey); // 매수 및 매도 주문 상세 (가격을 포함한 모든 주문 정보)

		BigDecimal myRemainingToken = myOrderDto.getOrderVolume(); // 미체결 수량 (매수 및 매도)
		BigDecimal myRemainingCash = (myOrderDto.getOrderSide() == OrderSide.BUY) ? myOrderDto.getTotalPrice() : BigDecimal.ZERO; // 미체결 금액(매수 - 총 주문 금액에서 시작)
		BigDecimal feeRate = new BigDecimal("0.0006");

		// 미체결 수량이 0보다 클 때 반복
		while (myRemainingToken.compareTo(BigDecimal.ZERO) > 0) { // myRemainingToken > 0

			// [Step 1] 호가창에서 첫 번째 주문을 가져옴 (=최적의 후보)
			// 1) 매수(me) -> 가장 싼 매도 (가격을 기준으로 오름차순 정렬)
			// 2) 매도(me) -> 가장 비싼 매수 (-가격을 기준으로 오름차순 정렬)
			String targetOrderId = counterOrderBook.first();
			if (targetOrderId == null) {
				// 호가창에 주문이 없으면 break
				break;
			}

			// [Step 2] 지정가 주문(me)인 경우, 가격 조건 확인 (내 가격 vs 상대방 가격)
			// 1) 매수(me) -> 내 가격 >= 상대방 가격 ("이 가격 이상으로는 안 사!")
			// 2) 매도(me) -> 내 가격 <= 상대방 가격 ("이 가격 이하로는 안 팔아!")
			// 호가창에서 상대방의 가격을 가져옴
			Double targetScore = counterOrderBook.getScore(targetOrderId);
			if (targetScore == null) {
				// 호가창에 상대방의 가격 정보가 없으면 break
				break;
			}

			// 상대방이 매수인 경우, 가격을 양수로 변환 (<-Sorted Set을 내림차순 정렬하기 위해 음수로 변환해서 넣음)
			BigDecimal targetPrice = (counterSide == OrderSide.BUY)
					? BigDecimal.valueOf(-targetScore)
					: BigDecimal.valueOf(targetScore);

			if (myOrderDto.getOrderType() == OrderType.LIMIT) {
				if (myOrderDto.getOrderSide() == OrderSide.BUY
					&& myOrderDto.getOrderPrice().compareTo(targetPrice) < 0) {
					// 매수(me)인 경우, 상대방 가격이 더 비싸면 break
					break;
				}
				if (myOrderDto.getOrderSide() == OrderSide.SELL
					&& myOrderDto.getOrderPrice().compareTo(targetPrice) > 0) {
					// 매도(me)인 경우, 상대방 가격이 더 싸면 break
					break;
				}
			}

			// 주문 상세에서 상대방의 주문 정보를 가져옴
			RedisOrderDTO targetOrder = infoMap.get(targetOrderId);
			if (targetOrder == null) {
				// 주문 상세에 상대방의 주문 정보가 없으면 해당 주문 제거 후, 다음 상대방 찾기 (continue)
				removeOrder(myOrderDto.getTokenId(), counterSide, targetOrderId);
				continue;
			}

			// [Step 3] 체결 내역 기록 및 자산 정산
			BigDecimal executedVolume = myRemainingToken.min(targetOrder.getVolume()); // 체결할 수량 (=나와 상대방 중 더 적은 수량)
			BigDecimal executedAmount = targetPrice.multiply(executedVolume); // 체결할 가격 = 상대방 단가(가장 유리) * 체결할 수량

			long tradeId = System.currentTimeMillis();
			long myOrderIdLong = IdFormatter.parseOrderId(myOrderId);
			long targetOrderIdLong = IdFormatter.parseOrderId(targetOrderId);

			long buyOrderId = (myOrderDto.getOrderSide() == OrderSide.BUY) ? myOrderIdLong : targetOrderIdLong;
			long sellOrderId = (myOrderDto.getOrderSide() == OrderSide.SELL) ? myOrderIdLong : targetOrderIdLong;

			try {
				orderRepository.p_process_transaction_hists(
					tradeId, buyOrderId, sellOrderId, targetPrice, executedVolume, feeRate
				);
				log.info("Trade Executed: Price {}, Volume {}, Amount {}", targetPrice, executedVolume, executedAmount);
			} catch (Exception e) {
				log.error("체결 내역 기록 중 오류 발생: {}", e.getMessage());
				break;
			}

			// 나의 미체결 수량 및 미체결 금액(매수만) 갱신
			myRemainingToken = myRemainingToken.subtract(executedVolume);
			if (myOrderDto.getOrderSide() == OrderSide.BUY) {
				myRemainingCash = myRemainingCash.subtract(executedAmount);
			}

			// 상대방 미체결 수량 및 미체결 금액(매수만) 갱신
			targetOrder.setVolume(targetOrder.getVolume().subtract(executedVolume));
			if (targetOrder.getSide() == OrderSide.BUY) {
				targetOrder.setRemainingCash(targetOrder.getRemainingCash().subtract(executedAmount));
			}

			// 상대방의 미체결 수량이
			// 1) 0보다 작거나 같으면, 해당 주문 제거
			// 2) 0보다 크면, 주문 정보 갱신(update)
			if (targetOrder.getVolume().compareTo(BigDecimal.ZERO) <= 0) {
				removeOrder(myOrderDto.getTokenId(), counterSide, targetOrderId);
			} else {
				infoMap.put(targetOrderId, targetOrder);
			}
		}

		// [Step 4] 매칭 반복 후 나의 미체결 수량 처리 (시장가 vs 지정가)
		if (myRemainingToken.compareTo(BigDecimal.ZERO) > 0) {
			if (myOrderDto.getOrderType() == OrderType.MARKET) {
				// 1) 시장가 주문인 경우, 미체결 수량 즉시 환불
				// orderRepository.p_cancel_order_and_refund(
				// 	IdFormatter.parseOrderId(myOrderId),
				// 	myRemainingToken,
				// 	myRemainingCash
				// );

				log.info("시장가 주문 잔량 환불 처리: OrderId {}, RemainingVolume {}", myOrderId, myRemainingToken);
			} else {
				// 2) 지정가 주문인 경우, 호가창 및 주문 상세에 등록(insert)
				String orderBookKey = RedisKeyManager.getOrderBookKey(myOrderDto.getTokenId(),
					myOrderDto.getOrderSide());
				RScoredSortedSet<String> orderBook = redissonClient.getScoredSortedSet(orderBookKey);

				double score = myOrderDto.getOrderPrice().doubleValue(); // 내 주문 가격
				if (myOrderDto.getOrderSide() == OrderSide.BUY) {
					// 매수인 경우, 내림차순 정렬을 위해 가격을 음수로 변환
					score = -score;
				}

				orderBook.add(score, myOrderId); // 해당 토큰 호가창에 내 주문 등록

				// RedisOrderDTO 생성자에 myRemainingCash를 추가로 넘겨줌
				RedisOrderDTO myRedisOrderDto = new RedisOrderDTO(
					myOrderId,
					myOrderDto.getWalletId(),
					myOrderDto.getTokenId(),
					myOrderDto.getOrderPrice(),
					myRemainingToken,
					myRemainingCash, // <- 이 부분이 정확히 들어가야 취소 시 돈을 돌려줄 수 있음
					myOrderDto.getOrderSide()
				);
				infoMap.put(myOrderId, myRedisOrderDto); // 해당 토큰 주문 상세에 내 주문 등록
			}
		}
	}

	// 호가창 및 주문 상세에서 주문 제거
	public void removeOrder(Long tokenId, OrderSide side, String orderId) {
		String bookKey = RedisKeyManager.getOrderBookKey(tokenId, side);
		String infoKey = RedisKeyManager.getOrderInfoKey(tokenId);

		// 1. Redisson을 통한 삭제 (executeMatching에서 쓰던 방식)
		redissonClient.getScoredSortedSet(bookKey).remove(orderId);
		redissonClient.getMap(infoKey).remove(orderId);

		// 2. Spring RedisTemplate을 통한 삭제 (EventListener가 넣은 방식 대응)
		// EventListener는 무조건 "ORD-"를 붙여서 저장하므로 똑같이 맞춰서 지워야 합니다.
		String formattedId = orderId.startsWith("ORD-") ? orderId : "ORD-" + orderId;
		redisTemplate.opsForZSet().remove(bookKey, formattedId);

		log.info("체결/취소로 인한 Redis 데이터 삭제 완료: {}", formattedId);
	}
}