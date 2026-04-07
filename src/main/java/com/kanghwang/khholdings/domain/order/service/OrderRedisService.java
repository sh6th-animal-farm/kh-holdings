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

import com.kanghwang.khholdings.domain.market.dto.OrderbookDTO;
import com.kanghwang.khholdings.domain.market.dto.TradeDTO;
import com.kanghwang.khholdings.domain.market.service.MarketDataService;
import com.kanghwang.khholdings.domain.order.dto.CancelRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderInfoDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.RefundRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import com.kanghwang.khholdings.domain.order.repository.OrderRepository;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.domain.order.type.OrderState;
import com.kanghwang.khholdings.domain.order.type.OrderType;
import com.kanghwang.khholdings.global.util.RedisKeyManager;
import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRedisService {

	private final OrderRepository orderRepository;
	private final SnowflakeIdGenerator snowflakeIdGenerator;
	private final RedissonClient redissonClient;
	private final RedisKeyManager redisKeyManager;
	private final MarketDataService marketDataService;
	private static final BigDecimal F_RATE = new BigDecimal("0.0006"); // 수수료 관리

	public void processOrder(OrderRequestDTO myOrderDTO) {

		// 1. 특정 토큰에 대한 분산 락 획득 (동시 매칭 방지)
		String lockKey = "lock:matching:" + myOrderDTO.getTokenId();
		RLock lock = redissonClient.getLock(lockKey);

		try {
			// 최대 2초 대기, 10초간 잠금 (10초 초과 시 자동으로 unlock)
			if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				try {
					executeMatching(myOrderDTO);
				} finally {
					if (lock.isLocked() && lock.isHeldByCurrentThread()) {
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
		OrderSide counterSide = (mySide == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;

		String myOrderBookKey = redisKeyManager.getOrderBookKey(myOrderDTO.getTokenId(), mySide);
		String counterOrderBookKey = redisKeyManager.getOrderBookKey(myOrderDTO.getTokenId(), counterSide);
		String orderInfoKey = redisKeyManager.getOrderInfoKey(myOrderDTO.getTokenId());

		RScoredSortedSet<Long> myOrderBook = redissonClient.getScoredSortedSet(myOrderBookKey); // 매수(매도) 호가창 ('가격':'주문번호')
		RScoredSortedSet<Long> counterOrderBook = redissonClient.getScoredSortedSet(counterOrderBookKey); // 매도(매수) 호가창 ('가격':'주문번호')
		RMap<Long, OrderRequestDTO> infoMap = redissonClient.getMap(orderInfoKey); // 매수 및 매도 주문 상세 ('주문번호':'주문상세(DTO)')

		// Race Condition 체크
		// 주문이 도착하기 전에 취소 요청이 먼저 온 경우 매칭 엔진에 진입 X
		String cancelKey = redisKeyManager.getCancelKey(myOrderId);
		if (Boolean.TRUE.equals(redissonClient.getBucket(cancelKey).isExists())) {
			log.warn("이미 취소 요청된 주문입니다. (ID: {})", myOrderId);
			redissonClient.getBucket(cancelKey).delete(); // 마킹 삭제
			return;
		}

		// 멱등성 체크
		// 이미 처리 중이거나 처리된 주문인지 확인
		if (infoMap.containsKey(myOrderId)) {
			log.warn("이미 매칭 엔진에 존재하는 주문입니다. 중복 처리를 방지합니다. ID: {}", myOrderId);
			return;
		}

		// 주문 요청 시, 호가창에 선 등록 (지정가만)
		if (myOrderDTO.getOrderType() == OrderType.LIMIT) {
			BigDecimal score = myOrderDTO.getOrderPrice(); // 내 주문 가격
			if (mySide == OrderSide.BUY) {
				// 매수인 경우, 내림차순 정렬을 위해 가격을 음수로 변환
				score = score.negate();
			}

			// 매칭 엔진 내 호가창 (-> 체결이 이루어짐)
			myOrderBook.add(score.doubleValue(), myOrderId);

			// 웹소켓 호가창 (-> 토큰 거래소에 호가를 띄워줌)
			updateAggrOrderBook(myOrderDTO.getTokenId(), mySide, myOrderDTO.getOrderPrice(), myOrderDTO.getOrderVolume());
		}

		infoMap.put(myOrderId, myOrderDTO); // 해당 토큰 주문 상세에 내 주문 등록

		while (true) {

			// [Step 0] 체결 종료 조건 확인
			if (myOrderDTO.getOrderType() == OrderType.MARKET && mySide == OrderSide.BUY) {
				// 1) 시장가 매수 : 미체결 금액이 0보다 작거나 같으면 break
				if (myOrderDTO.getRemainingCash().compareTo(BigDecimal.ZERO) <= 0) {
					log.info("[EXIT] 전액 체결로 매칭을 종료합니다.");
					break;
				}
			} else {
				if (myOrderDTO.getRemainingToken().compareTo(BigDecimal.ZERO) <= 0) {
					// 2) 그 외(지정가, 시장가 매도) : 미체결 수량이 0보다 작거나 같으면 break
					log.info("[EXIT] 전액 체결로 매칭을 종료합니다.");
					break;
				}
			}

			// [Step 1] 호가창에서 첫 번째 주문을 가져옴 (=최적의 후보)
			// 1) 매수(me) -> 가장 싼 매도 (가격을 기준으로 오름차순 정렬)
			// 2) 매도(me) -> 가장 비싼 매수 (-가격을 기준으로 오름차순 정렬)
			Long targetOrderId = counterOrderBook.first(); // 상대방의 주문
			if (targetOrderId == null) {
				// 호가창에 주문이 없으면 break
				log.info("[EXIT] 물량이 존재하지 않아 매칭을 종료합니다.");
				break;
			}

			// 호가창에서 상대방의 가격을 가져옴
			BigDecimal targetScore = BigDecimal.valueOf(counterOrderBook.getScore(targetOrderId)); // 상대방의 주문 가격
			if (targetScore == null) {
				// 호가창에 상대방의 가격 정보가 없으면 break
				log.info("[EXIT] 물량이 존재하지 않아 매칭을 종료합니다.");
				break;
			}

			// 상대방이 매수인 경우, 가격을 양수로 변환 (<-Sorted Set을 내림차순 정렬하기 위해 음수로 변환해서 넣음)
			BigDecimal targetPrice = (counterSide == OrderSide.BUY)
				? targetScore.negate()
				: targetScore;


			// [Step 2] 지정가 주문(me)인 경우, 가격 조건 확인 (내 가격 vs 상대방 가격)
			if (myOrderDTO.getOrderType() == OrderType.LIMIT) {
				// 1) 매수(me) -> 내 가격 >= 상대방 가격 ("이 가격 이상으로는 안 사!")
				if (mySide == OrderSide.BUY && myOrderDTO.getOrderPrice().compareTo(targetPrice) < 0) {
					// 매수는 상대방 가격이 더 비싸면 break
					log.info("[EXIT] 유리한 가격이 존재하지 않아 매칭을 종료합니다.");
					break;
				}

				// 2) 매도(me) -> 내 가격 <= 상대방 가격 ("이 가격 이하로는 안 팔아!")
				if (mySide == OrderSide.SELL && myOrderDTO.getOrderPrice().compareTo(targetPrice) > 0) {
					// 매도는 상대방 가격이 더 싸면 break
					log.info("[EXIT] 유리한 가격이 존재하지 않아 매칭을 종료합니다.");
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
			if (myOrderDTO.getOrderType() == OrderType.MARKET && mySide == OrderSide.BUY) {
				// 1) 시장가 매수
				// 내 미체결 금액으로 살 수 있는 수량
				// = (내 미체결 금액 / 상대방 주문 단가) -> 8자리 미만 버림
				BigDecimal maxBuyableVolume = myOrderDTO.getRemainingCash()
					.divide(targetPrice, 8, RoundingMode.DOWN);

				// 실제 체결 수량 = min(상대방 물량, 내가 살 수 있는 물량)
				executedVolume = targetVolume.min(maxBuyableVolume);
			} else {
				// 2) 그 외
				executedVolume = myOrderDTO.getRemainingToken().min(targetVolume);
			}

			// 체결할 수량이 없으면 종료
			// ex. (내 미체결 금액 / 상대방 주문 단가)의 결과가 0.000000001일 때, 9자리 수는 버림 해서 0개가 됨
			// -> 추후 미체결 금액에 대해 환불 처리
			if (executedVolume == null || executedVolume.compareTo(BigDecimal.ZERO) <= 0) {
				log.info("[EXIT] 체결 가능한 수량이 없어 매칭을 종료합니다.");
				break;
			}

			BigDecimal executedAmount = targetPrice.multiply(executedVolume); // 총 체결 = 상대방 단가(가장 유리) * 체결할 수량

			Long tradeId = snowflakeIdGenerator.nextId(); // 체결 번호 (매수-매도 체결에 대해 동일한 번호 부여)
			Long txId1 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매수자 현금 지출
			Long txId2 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매수자 토큰 유입
			Long txId3 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매도자 현금 유입
			Long txId4 = snowflakeIdGenerator.nextId();   // 거래 내역 번호 - 매도자 토큰 지출

			long tokenId = myOrderDTO.getTokenId();
			long buyWalletId = (mySide == OrderSide.BUY) ? myOrderDTO.getWalletId() : targetOrderDTO.getWalletId();
			long sellWalletId = (mySide == OrderSide.SELL) ? myOrderDTO.getWalletId() : targetOrderDTO.getWalletId();
			long buyOrderId = (mySide == OrderSide.BUY) ? myOrderId : targetOrderId;
			long sellOrderId = (mySide == OrderSide.SELL) ? myOrderId : targetOrderId;

			TransactionRequestDTO transactionDTO = TransactionRequestDTO.builder()
				.txId1(txId1)
				.txId2(txId2)
				.txId3(txId3)
				.txId4(txId4)
				.tradeId(tradeId)
				.buyOrderId(buyOrderId)
				.sellOrderId(sellOrderId)
				.targetPrice(targetPrice)
				.executedVolume(executedVolume)
				.feeRate(F_RATE)
				.createdAt(OffsetDateTime.now())
				.takerSide(mySide)
				.tokenId(tokenId)
				.buyWalletId(buyWalletId)
				.sellWalletId(sellWalletId)
				.build();

			// 체결이 발생할 때마다 Redis에 데이터 전달
			// -> DB 저장 / 웹소켓 실시간 전송

			// (1) 비동기 정산 및 이력 저장용 (Stream)
			// TradeWorker가 받아서 프로시저 실행
			// 현금 및 토큰 정산, 주문 내역 업데이트, 거래 내역 추가
			redissonClient.getStream(redisKeyManager.getTradeStreamKey()).add(StreamAddArgs.entry("data", transactionDTO));

			// (2) 실시간 프론트엔드 전파용 (Pub/Sub)
			// MarketWorker가 체결 내역을 받아서 웹소켓으로 전송
			TradeDTO tradeSummary = new TradeDTO(targetPrice, executedVolume, mySide, OffsetDateTime.now());
			redissonClient.getTopic(redisKeyManager.getTradeTopicKey(tokenId)).publish(tradeSummary);

			// (3) 토큰 실시간 리스트 제작, 캔들 생성
			marketDataService.processMarketUpdate(transactionDTO);

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
			if (myOrderDTO.getOrderType() == OrderType.LIMIT) {
				updateAggrOrderBook(myOrderDTO.getTokenId(), mySide, myOrderDTO.getOrderPrice(), executedVolume.negate());
			}
			infoMap.put(myOrderId, myOrderDTO);

			// 2) 상대방
			// 웹소켓 호가창 삭제 또는 업데이트 -> MarketWorker가 실시간으로 받아서 브라우저에 전송
			updateAggrOrderBook(tokenId, counterSide, targetPrice, executedVolume.negate());

			if (targetOrderDTO.getRemainingToken().compareTo(BigDecimal.ZERO) <= 0) {
				// 1) 0보다 작거나 같으면, 해당 주문을 호가창에서 제거 (매칭 X)
				log.info("주문 전량 체결 완료: OrderId {}", targetOrderId);
				removeOrder(myOrderDTO.getTokenId(), counterSide, targetOrderId);
			} else {
				// 2) 0보다 크면, 주문 상세 업데이트
				infoMap.put(targetOrderId, targetOrderDTO);
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
		// if (isCompleted || myOrderDTO.getOrderType() == OrderType.MARKET) {
		// 	removeOrder(myOrderDTO.getTokenId(), mySide, myOrderDTO.getOrderId());
		// }

		if (isCompleted) {
			// 지정가 매수인데 주문 요청 금액보다 싸게 사서 돈이 남은 경우 추가 환불
			if (myOrderDTO.getOrderType() == OrderType.LIMIT && myOrderDTO.getOrderSide() == OrderSide.BUY
				&& myOrderDTO.getRemainingCash().compareTo(BigDecimal.ZERO) > 0) {
				// 비동기 정산 및 이력 저장용 Stream에 저장 후 DB 프로시저 호출
				RefundRequestDTO refundRequestDTO = new RefundRequestDTO(snowflakeIdGenerator.nextId(), myOrderDTO.getOrderId(), myOrderDTO.getRemainingCash(), BigDecimal.ZERO);
				redissonClient.getStream(redisKeyManager.getTradeStreamKey())
					.add(StreamAddArgs.entry("data", refundRequestDTO));
				log.info("지정가 매수 차액 환불: 주문ID {}, 환불금액 {}", myOrderDTO.getOrderId(), myOrderDTO.getRemainingCash());
			}

			// 체결 완료 시, 호가창과 상세 정보에서 제거
			log.info("주문 전량 체결 완료: OrderId {}", myOrderDTO.getOrderId());
			removeOrder(myOrderDTO.getTokenId(), mySide, myOrderDTO.getOrderId());
		} else {
			// 부분 체결 시,
			if (myOrderDTO.getOrderType() == OrderType.MARKET) {
				// 1) 시장가 주문은 미체결 수량 즉시 환불
				// 비동기 정산 및 이력 저장용 Stream에 저장 후 DB 프로시저 호출
				RefundRequestDTO refundRequestDTO = new RefundRequestDTO(snowflakeIdGenerator.nextId(), myOrderDTO.getOrderId(), myOrderDTO.getRemainingCash(), myOrderDTO.getRemainingToken());
				redissonClient.getStream(redisKeyManager.getTradeStreamKey())
					.add(StreamAddArgs.entry("data", refundRequestDTO));

				log.info("시장가 주문 매칭 종료로 잔량 환불: OrderId {}", myOrderDTO.getOrderId());
				removeOrder(myOrderDTO.getTokenId(), mySide, myOrderDTO.getOrderId());
			} else {
				// 2) 지정가 주문은 이미 [Step 5]에서 업데이트
				log.info("지정가 주문 잔량 대기: OrderId {}, RemainingToken {}", myOrderDTO.getOrderId(), myOrderDTO.getRemainingToken());
			}
		}
	}

	// 웹소켓 호가창 가격 및 수량 전송
	private void updateAggrOrderBook(Long tokenId, OrderSide side, BigDecimal price, BigDecimal volume) {
		String orderbookAggrKey = redisKeyManager.getOrderBookAggrKey(tokenId, side);

		// ZSET: 가격 저장용 -> 자동 정렬
		RScoredSortedSet<String> aggrSet = redissonClient.getScoredSortedSet(orderbookAggrKey + ":index");
		// MAP: 수량 저장용
		RMap<String, BigDecimal> aggrMap = redissonClient.getMap(orderbookAggrKey);

		// 1. 수량 업데이트 ("가격" : "수량")
		String priceKey = price.stripTrailingZeros().toPlainString(); // 가격
		BigDecimal currentVolume = aggrMap.get(priceKey); // 수량

		if (currentVolume == null) currentVolume = BigDecimal.ZERO;

		BigDecimal updatedVolume = currentVolume.add(volume);
		aggrMap.put(priceKey, updatedVolume);

		// 2. 수량이 0 이하라면 필드 삭제, 아니면 업데이트 정보 전송
		String action = "UPDATE";
		if (updatedVolume.compareTo(BigDecimal.ZERO) <= 0) {
			// Redis에서 제거
			aggrSet.remove(priceKey);
			aggrMap.remove(price.toPlainString());
			updatedVolume = BigDecimal.ZERO;
			action = "DELETE";
		} else {
			// 잔량이 존재하면, 정렬 셋에 해당 가격 추가 또는 업데이트
			aggrSet.add(price.doubleValue(), priceKey);
			action = "UPDATE";
		}

		// 3. 웹소켓 전송을 위한 토픽 발행
		OrderbookDTO orderbookDTO = new OrderbookDTO(price, updatedVolume, side, action);
		redissonClient.getTopic(orderbookAggrKey).publish(orderbookDTO);
	}

	// 호가창 및 주문 상세에서 주문 제거
	public void removeOrder(Long tokenId, OrderSide side, Long orderId) {
		String bookKey = redisKeyManager.getOrderBookKey(tokenId, side);
		String infoKey = redisKeyManager.getOrderInfoKey(tokenId);

		// Redisson을 통한 삭제
		redissonClient.getScoredSortedSet(bookKey).remove(orderId); // 호가창에서 삭제
		redissonClient.getMap(infoKey).remove(orderId); // 주문 상세에서 삭제
	}

	// 주문 취소 (사용자가 직접)
	public void cancelOrder(CancelRequestDTO cancelDto) {
		Long tokenId = cancelDto.getTokenId();
		Long orderId = cancelDto.getOrderId();

		String orderInfoKey = redisKeyManager.getOrderInfoKey(tokenId);
		RMap<Long, OrderRequestDTO> infoMap = redissonClient.getMap(orderInfoKey);

		// 1. 주문 정보 확인
		OrderRequestDTO orderInfo = infoMap.get(orderId);

		if (orderInfo == null) {
			// DB에서 주문 정보를 가져오기
			OrderInfoDTO dbOrderInfo =  orderRepository.getOrderInfoById(orderId);

			// 이미 처리된 주문인 경우 종료
			if (dbOrderInfo == null || dbOrderInfo.getOrderState() != OrderState.NEW) {
				return;
			}

			// 취소 마킹
			// 주문이 지연되어 뒤늦게 들어왔을 때, 취소 마킹이 있으면 매칭 엔진 진입 X
			String cancelKey = redisKeyManager.getCancelKey(orderId);
			redissonClient.getBucket(cancelKey).set("CANCELLED", 15, TimeUnit.MINUTES); // 유효시간 15분

			log.info("매칭 엔진에 주문이 존재하지 않아 취소 마킹을 생성했습니다. (ID: {})", orderId);

			// DB 정보를 바탕으로 환불 스트림 발행
			RefundRequestDTO refundRequestDTO = new RefundRequestDTO(
				snowflakeIdGenerator.nextId(),
				orderId,
				dbOrderInfo.getTotalPrice(),
				dbOrderInfo.getOrderVolume()
			);

			redissonClient.getStream(redisKeyManager.getTradeStreamKey())
				.add(StreamAddArgs.entry("data", refundRequestDTO));

			log.info("Redis에 주문 정보가 없어 DB를 바탕으로 환불 스트림 발행 완료 (ID: {})", orderId);
			return;
		}

		// 2. Redis 작업 (호가창 및 주문 상세에서 제거)
		removeOrder(orderInfo.getTokenId(), orderInfo.getOrderSide(), orderId);
		updateAggrOrderBook(orderInfo.getTokenId(), orderInfo.getOrderSide(), orderInfo.getOrderPrice(), orderInfo.getRemainingToken().negate());

		// 3. 비동기 정산용 Stream 저장
		RefundRequestDTO refundRequestDTO = new RefundRequestDTO(
			snowflakeIdGenerator.nextId(),
			orderId,
			orderInfo.getRemainingCash(),
			orderInfo.getRemainingToken()
		);

		redissonClient.getStream(redisKeyManager.getTradeStreamKey())
			.add(StreamAddArgs.entry("data", refundRequestDTO));

		log.info("TradeWorker에 주문 취소 요청: OrderId {}", orderId);
	}
}
