package com.kanghwang.khholdings.domain.order.bot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.service.OrderService;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.domain.order.type.OrderType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingSimulationBot {

	private final OrderService orderService;
	private final RedissonClient redissonClient;
	private volatile boolean isRunning = false;

	private final Long[] testWalletIds = {
			1L, 2L, 3L, 4L, 5L,
			6L, 7L, 8L, 9L, 10L
	};

	@Async
	@Scheduled(fixedDelay = 200)
	public void runSimulation() {
		if (!isRunning) {
			return;
		}

		try {
//			OrderRequestDTO randomOrder = createRandomOrder();
// 			createAggressiveOrder();
			increasePriceStepByStep();

//			log.info(">>>> [BOT] 주문 생성 | 타입: {} | 사이드: {} | 가격: {} | 수량: {} | 총 주문액: {}",
//				randomOrder.getOrderType(),
//				randomOrder.getOrderSide(),
//				randomOrder.getOrderPrice(),
//				randomOrder.getOrderVolume(),
//				randomOrder.getTotalPrice());

//			orderService.placeOrder(randomOrder);

		} catch (Exception e) {
			log.error(">>>> [BOT] 주문 처리 중 오류: ", e);
		}
	}

	// 테스트: 45번 토큰 가격 100원씩 증가
	private BigDecimal currentBotPrice = new BigDecimal("5000");
	private final Long TARGET_TOKEN_ID = 45L;
	private void increasePriceStepByStep() {
		try {
			// 1. 가격 상승 (5000원부터 시작해서 매 호출마다 100원씩 상승)
			currentBotPrice = currentBotPrice.add(new BigDecimal("100"));

			BigDecimal volume = new BigDecimal("10.0"); // 체결 수량 10개 고정
			Long sellerId = 14L; // 메이커 (매도벽)
			Long buyerId = 3L;  // 테이커 (매수)

			// [STEP 1] 6번 지갑이 '지정가 매도' (벽 세우기)
			OrderRequestDTO sellOrder = OrderRequestDTO.builder()
				.walletId(sellerId)
				.tokenId(TARGET_TOKEN_ID)
				.orderSide(OrderSide.SELL)
				.orderType(OrderType.LIMIT)
				.orderPrice(currentBotPrice)
				.orderVolume(volume)
				.totalPrice(BigDecimal.ZERO)
				.build();

			orderService.placeOrder(sellOrder);

			// [STEP 2] 7번 지갑이 '시장가 매수' (즉시 체결)
			// 시장가 매수는 totalPrice에 지불할 최대 금액을 담음
			OrderRequestDTO buyOrder = OrderRequestDTO.builder()
				.walletId(buyerId)
				.tokenId(TARGET_TOKEN_ID)
				.orderSide(OrderSide.BUY)
				.orderType(OrderType.MARKET)
				.orderPrice(BigDecimal.ZERO)
				.orderVolume(BigDecimal.ZERO)
				.totalPrice(currentBotPrice.multiply(volume))
				.build();

			orderService.placeOrder(buyOrder);

		} catch (Exception e) {
			log.error(">>>> [BOT] 45번 토큰 가격 상승 루프 오류: {}", e.getMessage());
			// 에러 발생 시 너무 낮게 떨어지지 않도록 현재가 유지 혹은 로그 확인
		}
	}

	// 랜덤 주문
	private OrderRequestDTO createRandomOrder() {
		var random = java.util.concurrent.ThreadLocalRandom.current();

		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");
		List<TokenListDTO> tokens = new ArrayList<>(marketInfoMap.readAllValues());

		if (tokens.isEmpty()) {
			log.warn(">>>> [BOT] 마켓 정보가 없어 주문을 생성할 수 없습니다.");
			return null;
		}

		TokenListDTO targetToken = tokens.get(random.nextInt(tokens.size()));
		BigDecimal currentPrice = targetToken.getMarketPrice();

		if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) return null;

		// 테스트용 데이터
		Long walletId = testWalletIds[random.nextInt(testWalletIds.length)];
		OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
		OrderType type = (random.nextDouble() < 0.7) ? OrderType.LIMIT : OrderType.MARKET;

		// 가격 결정 로직: 현재가 기준 ±2% 범위 내에서 랜덤
		double variation = 0.98 + (random.nextDouble() * 0.04);
		BigDecimal orderPrice = currentPrice.multiply(BigDecimal.valueOf(variation))
				.setScale(0, RoundingMode.HALF_UP);

		BigDecimal volume = BigDecimal.valueOf(1 + (random.nextDouble() * 5.0))
				.setScale(4, RoundingMode.DOWN);

		var builder = OrderRequestDTO.builder()
			.walletId(walletId)
			.tokenId(targetToken.getTokenId())
			.orderSide(side)
			.orderType(type);

		if (type == OrderType.MARKET) {
			if (side == OrderSide.BUY) {
				builder.orderPrice(BigDecimal.ZERO);
				builder.orderVolume(BigDecimal.ZERO);
				builder.totalPrice(orderPrice.multiply(volume));
			} else {
				builder.orderPrice(BigDecimal.ZERO);
				builder.orderVolume(volume);
				builder.totalPrice(BigDecimal.ZERO);
			}
		} else {
			if (side == OrderSide.BUY) {
				builder.orderPrice(orderPrice);
				builder.orderVolume(volume);
				builder.totalPrice(orderPrice.multiply(volume));
			} else {
				builder.orderPrice(orderPrice);
				builder.orderVolume(volume);
				builder.totalPrice(BigDecimal.ZERO);
			}
		}

		return builder.build();
	}

	// 공격적인 주문 (벽 세우기 + 즉시 체결로 거래량 및 가격 유도)
	private void createAggressiveOrder() {
		var random = java.util.concurrent.ThreadLocalRandom.current();
		
		// 1. Redis에서 현재 활성화된 모든 토큰 목록 가져오기
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");
		List<TokenListDTO> tokens = new ArrayList<>(marketInfoMap.readAllValues());

		if (tokens.isEmpty()) {
			log.warn(">>>> [BOT] 마켓 정보가 없어 MM 주문을 생성하지 못했습니다.");
			return;
		}

		// 2. 무작위로 하나의 타겟 토큰 선정 (이제 1번 토큰 고정 아님!)
		TokenListDTO targetToken = tokens.get(random.nextInt(tokens.size()));
		Long tokenId = targetToken.getTokenId();
		BigDecimal currentPrice = targetToken.getMarketPrice();

		if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) return;

		// 3. 현실적인 가격/수량 결정 (현재가 기준 ±3% 범위)
		double variation = 0.97 + (random.nextDouble() * 0.06);
		BigDecimal targetPrice = currentPrice.multiply(BigDecimal.valueOf(variation))
				.setScale(0, RoundingMode.HALF_UP);

		// 거래량도 1~10 사이로 무작위 (더 풍부하게)
		BigDecimal volume = BigDecimal.valueOf(1.0 + random.nextDouble() * 9.0)
				.setScale(4, RoundingMode.DOWN);

		// 4. 메이커(벽)와 테이커(체결) 지갑 무작위 선정
		Long makerId = testWalletIds[random.nextInt(testWalletIds.length)];
		Long takerId = testWalletIds[random.nextInt(testWalletIds.length)];
		while (makerId.equals(takerId)) {
			takerId = testWalletIds[random.nextInt(testWalletIds.length)];
		}

		// 5. 매수/매도 방향 결정
		OrderSide wallSide = (targetPrice.compareTo(currentPrice) > 0) ? OrderSide.SELL : OrderSide.BUY;
		OrderSide takerSide = (wallSide == OrderSide.SELL) ? OrderSide.BUY : OrderSide.SELL;

		// [STEP 1] 지정가 주문으로 '벽' 세우기 (메이커)
		var limitBuilder = OrderRequestDTO.builder()
				.walletId(makerId)
				.tokenId(tokenId)
				.orderSide(wallSide)
				.orderType(OrderType.LIMIT)
				.orderPrice(targetPrice)
				.orderVolume(volume);

		if (wallSide == OrderSide.BUY) {
			limitBuilder.totalPrice(targetPrice.multiply(volume));
		} else {
			limitBuilder.totalPrice(BigDecimal.ZERO);
		}
		
		try {
			orderService.placeOrder(limitBuilder.build());
		} catch (Exception e) {
			log.error(">>>> [BOT] MM 벽 주문 생성 실패: {}", e.getMessage());
		}

		// [STEP 2] 시장가 주문으로 즉시 체결 (테이커)
		var marketBuilder = OrderRequestDTO.builder()
				.walletId(takerId)
				.tokenId(tokenId)
				.orderSide(takerSide)
				.orderType(OrderType.MARKET)
				.orderPrice(BigDecimal.ZERO);

		if (takerSide == OrderSide.BUY) {
			marketBuilder.orderVolume(BigDecimal.ZERO);
			marketBuilder.totalPrice(targetPrice.multiply(volume));
		} else {
			marketBuilder.orderVolume(volume);
			marketBuilder.totalPrice(BigDecimal.ZERO);
		}
		
		try {
			orderService.placeOrder(marketBuilder.build());
		} catch (Exception e) {
			log.error(">>>> [BOT] MM 체결 주문 생성 실패: {}", e.getMessage());
		}
	}
}

// 6번과 7번 중 누가 먼저 벽을 세울지도 랜덤으로!
//Long makerId = random.nextBoolean() ? 6L : 7L;
//Long takerId = (makerId == 6L) ? 7L : 6L;
//
//limitBuilder.walletId(makerId); // 메이커 지정
//marketBuilder.walletId(takerId); // 테이커 지정