package com.kanghwang.khholdings.domain.order;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.RedisOrderDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.global.util.IdFormatter;
import com.kanghwang.khholdings.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRedisService {

	private final RedissonClient redissonClient;
	private final OrderRepository orderRepository;
	private final RedisTemplate<String, String> redisTemplate;

	public void processOrder(String orderId, OrderRequestDTO dto) {
		// 1. 특정 토큰에 대한 분산 락 획득 (동시 매칭 방지)
		String lockKey = "lock:matching:" + dto.getTokenId();
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 최대 5초 대기, 10초간 잠금 점유
			if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				try {
					executeMatching(orderId, dto);
				} finally {
					lock.unlock();
				}
			}
		} catch (InterruptedException e) {
			log.error("Matching lock error: {}", e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	private void executeMatching(String orderId, OrderRequestDTO dto) {
		OrderSide counterSide = (dto.getOrderSide() == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;
		String counterBookKey = RedisKeyManager.getOrderBookKey(dto.getTokenId(), counterSide);
		String infoKey = RedisKeyManager.getOrderInfoKey(dto.getTokenId());

		RScoredSortedSet<String> counterBook = redissonClient.getScoredSortedSet(counterBookKey);
		RMap<String, RedisOrderDTO> infoMap = redissonClient.getMap(infoKey);

		BigDecimal myRemainingVol = dto.getOrderVolume();
		// 내 주문이 BUY일 경우, 사용할 수 있는 총 현금(totalPrice)에서 시작
		BigDecimal myRemainingCash = (dto.getOrderSide() == OrderSide.BUY) ? dto.getTotalPrice() : BigDecimal.ZERO;
		BigDecimal feeRate = new BigDecimal("0.0006");

		while (myRemainingVol.compareTo(BigDecimal.ZERO) > 0) {
//			String targetId = (dto.getOrderSide() == OrderSide.BUY) ? counterBook.first() : counterBook.last();

			String targetId = counterBook.first();
			if (targetId == null) break;

//			Double targetPriceDouble = counterBook.getScore(targetId);
			Double targetScore = counterBook.getScore(targetId);
			if (targetScore == null) break;

			BigDecimal targetPrice = (counterSide == OrderSide.BUY)
					? BigDecimal.valueOf(-targetScore)
					: BigDecimal.valueOf(targetScore);

			if (dto.getOrderSide() == OrderSide.BUY && dto.getOrderPrice().compareTo(targetPrice) < 0) break;
			if (dto.getOrderSide() == OrderSide.SELL && dto.getOrderPrice().compareTo(targetPrice) > 0) break;

			RedisOrderDTO targetOrder = infoMap.get(targetId);
			if (targetOrder == null) {
				removeOrder(dto.getTokenId(), counterSide, targetId);
				continue;
			}

			BigDecimal execVol = myRemainingVol.min(targetOrder.getVolume());
			// 이번 체결에 소모되는 현금 계산
			BigDecimal execAmount = targetPrice.multiply(execVol);

			long tradeId = System.currentTimeMillis();
			long myIdLong = IdFormatter.parseOrderId(orderId);
			long targetIdLong = IdFormatter.parseOrderId(targetId);

			long buyOrderId = (dto.getOrderSide() == OrderSide.BUY) ? myIdLong : targetIdLong;
			long sellOrderId = (dto.getOrderSide() == OrderSide.SELL) ? myIdLong : targetIdLong;

			try {
				orderRepository.p_process_transaction_hists(
						tradeId, buyOrderId, sellOrderId, targetPrice, execVol, feeRate
				);
				log.info("Trade Executed: Price {}, Vol {}", targetPrice, execVol);
			} catch (Exception e) {
				log.error("DB 정산 중 오류 발생: {}", e.getMessage());
				break;
			}

			// 수량 차감 및 내 현금 잔액 갱신
			myRemainingVol = myRemainingVol.subtract(execVol);
			if (dto.getOrderSide() == OrderSide.BUY) {
				myRemainingCash = myRemainingCash.subtract(execAmount);
			}

			// 상대방 수량 차감 및 상대방이 매수자라면 현금 잔액도 갱신
			targetOrder.setVolume(targetOrder.getVolume().subtract(execVol));
			if (targetOrder.getSide() == OrderSide.BUY) {
				targetOrder.setRemainingCash(targetOrder.getRemainingCash().subtract(execAmount));
			}

			if (targetOrder.getVolume().compareTo(BigDecimal.ZERO) <= 0) {
				removeOrder(dto.getTokenId(), counterSide, targetId);
			} else {
				infoMap.put(targetId, targetOrder);
			}
		}

		// 7. 매칭 후 남은 수량이 있다면 호가창에 내 주문 등록
		if (myRemainingVol.compareTo(BigDecimal.ZERO) > 0) {
			String myBookKey = RedisKeyManager.getOrderBookKey(dto.getTokenId(), dto.getOrderSide());
			RScoredSortedSet<String> myBook = redissonClient.getScoredSortedSet(myBookKey);

			double score = dto.getOrderPrice().doubleValue();
			if (dto.getOrderSide() == OrderSide.BUY) {
				score = -score;
			}

			myBook.add(score, orderId);

			// RedisOrderDTO 생성자에 myRemainingCash를 추가로 넘겨줌
			RedisOrderDTO myRedisOrder = new RedisOrderDTO(
					orderId,
					dto.getWalletId(),
					dto.getTokenId(),
					dto.getOrderPrice(),
					myRemainingVol,
					myRemainingCash, // <- 이 부분이 정확히 들어가야 취소 시 돈을 돌려줄 수 있음
					dto.getOrderSide()
			);
			infoMap.put(orderId, myRedisOrder);
		}
	}

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