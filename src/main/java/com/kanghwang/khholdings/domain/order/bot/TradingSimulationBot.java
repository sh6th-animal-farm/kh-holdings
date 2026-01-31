package com.kanghwang.khholdings.domain.order.bot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
			createAggressiveOrder();

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

	// 특정 토큰 주문
	private void createAggressiveOrder() {
		var random = java.util.concurrent.ThreadLocalRandom.current();
		long tokenId = 1L;

		// 1. 특정 토큰의 현재가 가져오기
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");
		TokenListDTO targetToken = marketInfoMap.get(tokenId);

		if (targetToken == null || targetToken.getMarketPrice() == null) {
			log.warn(">>>> [BOT] 타겟 토큰 정보를 찾을 수 없습니다.");
			return;
		}

		BigDecimal currentPrice = targetToken.getMarketPrice();

		// 현재가 대비 ±5% 범위
		double variation = 0.95 + (random.nextDouble() * 0.1);
		BigDecimal targetPrice = currentPrice.multiply(BigDecimal.valueOf(variation))
				.setScale(0, RoundingMode.HALF_UP);

		BigDecimal volume = BigDecimal.valueOf(1.0 + random.nextDouble() * 2.0)
				.setScale(4, RoundingMode.DOWN);

		// 2. 매수/매도 방향 결정
		// 타겟가가 현재가보다 높으면 매도벽을 만들고 매수로 긁음 (상승)
		// 타겟가가 현재가보다 낮으면 매수벽을 만들고 매도로 긁음 (하락)
		OrderSide wallSide = (targetPrice.compareTo(currentPrice) > 0) ? OrderSide.SELL : OrderSide.BUY;
		OrderSide takerSide = (wallSide == OrderSide.SELL) ? OrderSide.BUY : OrderSide.SELL;

		// [STEP 1] 지정가 주문으로 '체결 대상' 생성 (벽 세우기)
		var limitBuilder = OrderRequestDTO.builder()
				.walletId(6L)
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
		orderService.placeOrder(limitBuilder.build());

		// [STEP 2] 시장가 주문으로 즉시 체결 (벽 부수기)
		// 시장가 매수는 totalPrice가 필요하고, 시장가 매도는 orderVolume이 필요함
		var marketBuilder = OrderRequestDTO.builder()
				.walletId(7L)
				.tokenId(tokenId)
				.orderSide(takerSide)
				.orderType(OrderType.MARKET)
				.orderPrice(BigDecimal.ZERO); // 시장가는 가격 0

		if (takerSide == OrderSide.BUY) {
			// 시장가 매수: 수량 0, 총액 입력
			marketBuilder.orderVolume(BigDecimal.ZERO);
			marketBuilder.totalPrice(targetPrice.multiply(volume));
		} else {
			// 시장가 매도: 수량 입력, 총액 0
			marketBuilder.orderVolume(volume);
			marketBuilder.totalPrice(BigDecimal.ZERO);
		}
		orderService.placeOrder(marketBuilder.build());
	}
}

// 6번과 7번 중 누가 먼저 벽을 세울지도 랜덤으로!
//Long makerId = random.nextBoolean() ? 6L : 7L;
//Long takerId = (makerId == 6L) ? 7L : 6L;
//
//limitBuilder.walletId(makerId); // 메이커 지정
//marketBuilder.walletId(takerId); // 테이커 지정