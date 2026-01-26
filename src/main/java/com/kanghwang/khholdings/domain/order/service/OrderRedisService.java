package com.kanghwang.khholdings.domain.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.RefundRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.domain.order.type.OrderType;
import com.kanghwang.khholdings.global.dto.RealTimeEvent;
import com.kanghwang.khholdings.global.util.RedisKeyManager;
import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRedisService {

	private final SnowflakeIdGenerator snowflakeIdGenerator;
	private final RedissonClient redissonClient;
	private final RedisKeyManager redisKeyManager;
	private static final BigDecimal F_RATE = new BigDecimal("0.0006"); // 수수료 관리

	public void processOrder(OrderRequestDTO myOrderDTO) {

		// 1. 특정 토큰에 대한 분산 락 획득 (동시 매칭 방지)
		String lockKey = "lock:matching:" + myOrderDTO.getTokenId();
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 최대 2초 대기, 10초간 잠금 (10초 초과 시 자동으로 unlock)
			if (lock.tryLock(2, 10, TimeUnit.SECONDS)) {
				try {
					executeMatching(myOrderDTO);
				} finally {
					if (lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				}
			}
		} catch (InterruptedException e) {
			log.error("Matching lock error: {}", e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	// 매칭 엔진
	private void executeMatching(OrderRequestDTO myOrderDTO) {

		Long myOrderId = myOrderDTO.getOrderId();
		OrderSide mySide = myOrderDTO.getOrderSide();
		OrderSide counterSide = (myOrderDTO.getOrderSide() == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;

		String myOrderBookKey = redisKeyManager.getOrderBookKey(myOrderDTO.getTokenId(), mySide);
		String counterOrderBookKey = redisKeyManager.getOrderBookKey(myOrderDTO.getTokenId(), counterSide);
		String orderInfoKey = redisKeyManager.getOrderInfoKey(myOrderDTO.getTokenId());

		RScoredSortedSet<Long> myOrderBook = redissonClient.getScoredSortedSet(myOrderBookKey); // 매수(매도) 호가창 ('가격':'주문번호')
		RScoredSortedSet<Long> counterOrderBook = redissonClient.getScoredSortedSet(counterOrderBookKey); // 매도(매수) 호가창 ('가격':'주문번호')
		RMap<Long, OrderRequestDTO> infoMap = redissonClient.getMap(orderInfoKey); // 매수 및 매도 주문 상세 ('주문번호':'주문상세(DTO)')

		// 주문 요청 시, 호가창에 선등록 (후체결)
		BigDecimal score = myOrderDTO.getOrderPrice(); // 내 주문 가격
		if (myOrderDTO.getOrderSide() == OrderSide.BUY) {
			// 매수인 경우, 내림차순 정렬을 위해 가격을 음수로 변환
			score = score.negate();
		}

		if (myOrderDTO.getOrderType() == OrderType.LIMIT) {
			// 처음부터 지정가만 호가창에 들어가도록
			myOrderBook.add(score.doubleValue(), myOrderId); // 해당 토큰 호가창에 내 주문 등록
		}

		infoMap.put(myOrderId, myOrderDTO); // 해당 토큰 주문 상세에 내 주문 등록

		while (true) {

			// [Step 0] 체결 종료 조건 확인
			if (myOrderDTO.getOrderType() == OrderType.MARKET && myOrderDTO.getOrderSide() == OrderSide.BUY) {
				// 1) 시장가 매수 : 미체결 금액이 0보다 작거나 같으면 break
				if (myOrderDTO.getRemainingCash().compareTo(BigDecimal.ZERO) <= 0) {
					break;
				}
			} else {
				if (myOrderDTO.getRemainingToken().compareTo(BigDecimal.ZERO) <= 0) {
					// 2) 그 외(지정가, 시장가 매도) : 미체결 수량이 0보다 작거나 같으면 break
					break;
				}
			}

			// [Step 1] 호가창에서 첫 번째 주문을 가져옴 (=최적의 후보)
			// 1) 매수(me) -> 가장 싼 매도 (가격을 기준으로 오름차순 정렬)
			// 2) 매도(me) -> 가장 비싼 매수 (-가격을 기준으로 오름차순 정렬)
			Long targetOrderId = counterOrderBook.first(); // 상대방의 주문
			if (targetOrderId == null) {
				// 호가창에 주문이 없으면 break
				break;
			}

			// 호가창에서 상대방의 가격을 가져옴
			BigDecimal targetScore = BigDecimal.valueOf(counterOrderBook.getScore(targetOrderId)); // 상대방의 주문 가격
			if (targetScore == null) {
				// 호가창에 상대방의 가격 정보가 없으면 break
				break;
			}

			// 상대방이 매수인 경우, 가격을 양수로 변환 (<-Sorted Set을 내림차순 정렬하기 위해 음수로 변환해서 넣음)
			BigDecimal targetPrice = (counterSide == OrderSide.BUY)
					? targetScore.negate()
					: targetScore;


			// [Step 2] 지정가 주문(me)인 경우, 가격 조건 확인 (내 가격 vs 상대방 가격)
			if (myOrderDTO.getOrderType() == OrderType.LIMIT) {
				// 1) 매수(me) -> 내 가격 >= 상대방 가격 ("이 가격 이상으로는 안 사!")
				if (myOrderDTO.getOrderSide() == OrderSide.BUY
					&& myOrderDTO.getOrderPrice().compareTo(targetPrice) < 0) {
					// 매수는 상대방 가격이 더 비싸면 break
					break;
				}

				// 2) 매도(me) -> 내 가격 <= 상대방 가격 ("이 가격 이하로는 안 팔아!")
				if (myOrderDTO.getOrderSide() == OrderSide.SELL
					&& myOrderDTO.getOrderPrice().compareTo(targetPrice) > 0) {
					// 매도는 상대방 가격이 더 싸면 break
					break;
				}
			}

			// 주문 상세에서 상대방의 주문 정보를 가져옴
			OrderRequestDTO targetOrderDTO = infoMap.get(targetOrderId);
			if (targetOrderDTO == null) {
				// 주문 상세에 상대방의 주문 정보가 없으면 해당 주문 제거 후, 다음 상대방 찾기 (continue)
				removeOrder(myOrderDTO.getTokenId(), counterSide, targetOrderId);
				continue;
			}

			// [Step 3] 체결 내역 기록
			BigDecimal executedVolume = null; // 체결할 수량
			BigDecimal targetVolume = targetOrderDTO.getRemainingToken(); // 상대방이 팔려는 수량
			if (myOrderDTO.getOrderType() == OrderType.MARKET && myOrderDTO.getOrderSide() == OrderSide.BUY) {
				// 1) 시장가 매수
				// 내 미체결 금액으로 살 수 있는 수량 (현금 / 가격)
				BigDecimal maxBuyableVolume = myOrderDTO.getRemainingCash()
					.divide(targetPrice, 8, RoundingMode.DOWN);

				// 실제 체결 수량 = min(상대방 물량, 내가 살 수 있는 물량)
				executedVolume = targetVolume.min(maxBuyableVolume);
			} else {
				// 2) 그 외
				executedVolume = myOrderDTO.getRemainingToken().min(targetVolume);
			}
			
			// 체결할 수량이 없으면 종료
			if (executedVolume == null || executedVolume.compareTo(BigDecimal.ZERO) <= 0) {
				log.info("체결 가능한 수량이 없어 매칭을 종료합니다.");
				break;
			}
			
			BigDecimal executedAmount = targetPrice.multiply(executedVolume); // 총 체결 = 상대방 단가(가장 유리) * 체결할 수량

			Long tradeId = snowflakeIdGenerator.nextId(); // 체결 번호 (매수-매도 체결에 대해 동일한 번호 부여)
			Long txId1 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매수자 현금 지출
			Long txId2 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매수자 토큰 유입
			Long txId3 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매도자 현금 유입
			Long txId4 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매도자 토큰 지출

			long tokenId = myOrderDTO.getTokenId();
			long buyWalletId = (myOrderDTO.getOrderSide() == OrderSide.BUY) ? myOrderDTO.getWalletId() : targetOrderDTO.getWalletId();
			long sellWalletId = (myOrderDTO.getOrderSide() == OrderSide.SELL) ? myOrderDTO.getWalletId() : targetOrderDTO.getWalletId();
			long buyOrderId = (myOrderDTO.getOrderSide() == OrderSide.BUY) ? myOrderId : targetOrderId;
			long sellOrderId = (myOrderDTO.getOrderSide() == OrderSide.SELL) ? myOrderId : targetOrderId;

			TransactionRequestDTO transactionDTO = new TransactionRequestDTO(txId1, txId2, txId3, txId4, tradeId, buyOrderId, sellOrderId, targetPrice, executedVolume, F_RATE,
				OffsetDateTime.now(), mySide, tokenId, buyWalletId, sellWalletId);

			// 체결이 발생할 때마다 Redis에 데이터 전달
			// -> DB 저장 / 웹소켓 실시간 전송

			// (1) 비동기 정산 및 이력 저장용 (Stream)
			// TradeWorker가 받아서 프로시저 실행
			// 현금 및 토큰 정산, 주문 내역 업데이트, 거래 내역 추가
			redissonClient.getStream(redisKeyManager.getPrefix() + "trade:stream:")
					.add(StreamAddArgs.entry("data", transactionDTO));

			// [MarketWorker - 체결]
			// (2) 실시간 프론트엔드 전파용 (Pub/Sub)
			// 체결 내역을 MarketWorker가 받아서 웹소켓으로 전송
			redissonClient.getTopic(redisKeyManager.getPrefix() + "trade:topic:" + myOrderDTO.getTokenId())
					.publish(transactionDTO);

			// (3) 현재가 갱신 (예: "ticker:last_price:{tokenId}")
			// Redis에 해당 토큰의 마지막 체결가를 저장
			redissonClient.getBucket(redisKeyManager.getPrefix() + "ticker:last_price:" + myOrderDTO.getTokenId())
					.set(targetPrice);

			log.info("체결: Price {}, Volume {}, Amount {}", targetPrice.toPlainString(), executedVolume.toPlainString(), executedAmount.toPlainString());
			log.info("Worker에게 나머지 작업 전달: TradeID {}", tradeId);

			// [Step 4] 자산 정산 (미체결 수량 및 금액 갱신)
			// 1) 나
			if (myOrderDTO.getOrderSide() == OrderSide.SELL
					|| (myOrderDTO.getOrderSide() == OrderSide.BUY && myOrderDTO.getOrderType() == OrderType.LIMIT)) {
				myOrderDTO.setRemainingToken(myOrderDTO.getRemainingToken().subtract(executedVolume));
			}
			if (myOrderDTO.getOrderSide() == OrderSide.BUY) {
				myOrderDTO.setRemainingCash(myOrderDTO.getRemainingCash().subtract(targetPrice.multiply(executedVolume)));
			}
			// 2) 상대방
			if (targetOrderDTO.getOrderSide() == OrderSide.SELL
					|| (targetOrderDTO.getOrderSide() == OrderSide.BUY && targetOrderDTO.getOrderType() == OrderType.LIMIT)) {
				targetOrderDTO.setRemainingToken(targetOrderDTO.getRemainingToken().subtract(executedVolume));
			}
			if (targetOrderDTO.getOrderSide() == OrderSide.BUY) {
				targetOrderDTO.setRemainingCash(targetOrderDTO.getRemainingCash().subtract(targetPrice.multiply(executedVolume)));
			}

			// [Step 5] 호가창 업데이트
			// 1) 나
			infoMap.put(myOrderId, myOrderDTO);

			// 2) 상대방
			if (targetOrderDTO.getRemainingToken().compareTo(BigDecimal.ZERO) <= 0) {
				// 1) 0보다 작거나 같으면, 해당 주문을 호가창에서 제거 (매칭 X)
				log.info("주문 전량 체결 완료: OrderId {}", targetOrderId);
				removeOrder(myOrderDTO.getTokenId(), counterSide, targetOrderId);
			} else {
				// 2) 0보다 크면, 주문 상세 업데이트
				infoMap.put(targetOrderId, targetOrderDTO);

				// 지정가 주문 정보를 실시간으로 전파 -> MarketWorker가 받아서 웹소켓으로 전송
				if (targetOrderDTO.getOrderType() == OrderType.LIMIT) {
					RealTimeEvent<OrderRequestDTO> event = new RealTimeEvent<>("UPDATE", targetOrderDTO);
					redissonClient.getTopic(redisKeyManager.getPrefix() + "order:topic:" + targetOrderDTO.getTokenId()).publish(event);
				}
			}
		}

		// [Step 6] 매칭 종료 후 주문 최종 처리
		finalizeOrderProcess(myOrderDTO, mySide);
	}

	private void finalizeOrderProcess(OrderRequestDTO myOrderDTO, OrderSide mySide) {

		// 주문이 완전히 체결되었는지 확인
		boolean isCompleted = false;
		if (myOrderDTO.getOrderType() == OrderType.MARKET && myOrderDTO.getOrderSide() == OrderSide.BUY) {
			// 시장가 매수: 남은 현금이 없으면 완료
			isCompleted = myOrderDTO.getRemainingCash().compareTo(BigDecimal.ZERO) <= 0;
		} else {
			// 그 외: 남은 수량이 없으면 완료
			isCompleted = myOrderDTO.getRemainingToken().compareTo(BigDecimal.ZERO) <= 0;
		}

		// 완료된 주문이거나, 시장가 주문(잔량 즉시 환불 대상)이면 Redis 호가창에서 먼저 지움
		if (isCompleted || myOrderDTO.getOrderType() == OrderType.MARKET) {
			removeOrder(myOrderDTO.getTokenId(), mySide, myOrderDTO.getOrderId());
		}

		if (isCompleted) {
			// 지정가 매수인데 주문 요청 금액보다 싸게 사서 돈이 남은 경우 추가 환불
			if (myOrderDTO.getOrderType() == OrderType.LIMIT
					&& myOrderDTO.getOrderSide() == OrderSide.BUY
					&& myOrderDTO.getRemainingCash().compareTo(BigDecimal.ZERO) > 0) {
				// 비동기 정산 및 이력 저장용 Stream에 저장 후 DB 프로시저 호출
				RefundRequestDTO refundRequestDTO = new RefundRequestDTO(snowflakeIdGenerator.nextId(), myOrderDTO.getOrderId(), myOrderDTO.getRemainingCash(), BigDecimal.ZERO);
				redissonClient.getStream(redisKeyManager.getPrefix() + "trade:stream:")
					.add(StreamAddArgs.entry("data", refundRequestDTO));
				log.info("지정가 매수 차액 환불: 주문ID {}, 환불금액 {}", myOrderDTO.getOrderId(), myOrderDTO.getRemainingCash());
			}
			// 체결 완료 시, 호가창과 상세 정보에서 제거
			log.info("주문 전량 체결 완료: OrderId {}", myOrderDTO.getOrderId());
		} else {
			// 부분 체결 시,
			if (myOrderDTO.getOrderType() == OrderType.MARKET) {
				// 1) 시장가 주문은 미체결 수량 즉시 환불
				// 비동기 정산 및 이력 저장용 Stream에 저장 후 DB 프로시저 호출
				RefundRequestDTO refundRequestDTO = new RefundRequestDTO(snowflakeIdGenerator.nextId(), myOrderDTO.getOrderId(), myOrderDTO.getRemainingCash(), myOrderDTO.getRemainingToken());
				redissonClient.getStream(redisKeyManager.getPrefix() + "trade:stream:")
					.add(StreamAddArgs.entry("data", refundRequestDTO));
				log.info("시장가 주문 매칭 종료로 잔량 환불: OrderId {}", myOrderDTO.getOrderId());
			} else {
				// 2) 지정가 주문은 이미 [Step 5]에서 업데이트
				log.info("지정가 주문 잔량 대기: OrderId {}, RemainingToken {}", myOrderDTO.getOrderId(), myOrderDTO.getRemainingToken());

				// 웹소켓에서 주문(호가) 정보 업데이트
				RealTimeEvent<OrderRequestDTO> event = new RealTimeEvent<>("UPDATE", myOrderDTO);
				redissonClient.getTopic(redisKeyManager.getPrefix() + "order:topic:" + myOrderDTO.getTokenId()).publish(event);
			}
		}
	}

	// 호가창 및 주문 상세에서 주문 제거
	public void removeOrder(Long tokenId, OrderSide side, Long orderId) {
		String bookKey = redisKeyManager.getOrderBookKey(tokenId, side);
		String infoKey = redisKeyManager.getOrderInfoKey(tokenId);

		// Redisson을 통한 삭제
		redissonClient.getScoredSortedSet(bookKey).remove(orderId); // 호가창에서 삭제
		redissonClient.getMap(infoKey).remove(orderId); // 주문 상세에서 삭제

		// 웹소켓에서 주문(호가) 정보 삭제
		RealTimeEvent<Long> event = new RealTimeEvent<>("DELETE", orderId);
		redissonClient.getTopic(redisKeyManager.getPrefix() + "order:topic:" + tokenId).publish(event);
	}

	// 주문 취소 (사용자가 직접)
	public boolean cancelOrder(Long tokenId, Long orderId){
		String orderInfoKey = redisKeyManager.getOrderInfoKey(tokenId);
		RMap<Long, OrderRequestDTO> infoMap = redissonClient.getMap(orderInfoKey);

		// 1. 주문 정보 확인 (이미 취소되었거나 없을 경우)
		OrderRequestDTO orderInfo = infoMap.get(orderId); // 주문 정보 가져오기
		if (orderInfo == null) {
			return false; // 이미 처리된 주문이거나 존재하지 않음
		}

		try {
			// 2. Redis 호가창 및 주문 상세에서 주문 제거
			removeOrder(orderInfo.getTokenId(), orderInfo.getOrderSide(), orderId);

			// 3. 비동기 정산 및 이력 저장용 Stream에 저장 후 DB 프로시저 호출
			RefundRequestDTO refundRequestDTO = new RefundRequestDTO(snowflakeIdGenerator.nextId(), orderId, orderInfo.getRemainingCash(), orderInfo.getRemainingToken());
			redissonClient.getStream(redisKeyManager.getPrefix() + "trade:stream:")
				.add(StreamAddArgs.entry("data", refundRequestDTO));

			log.info("TradeWorker에 주문 취소 요청: OrderId {}", orderId);
			return true;
		} catch (Exception e) {
			log.error("취소 처리 중 오류 발생: {}", e.getMessage());
			return false;
		}
	}
}
