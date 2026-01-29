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
import com.kanghwang.khholdings.global.util.RedisKeyManager;

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
	private final RedisKeyManager redisKeyManager;
	private volatile boolean isRunning = false;

	private final Long[] testWalletIds = {
			1L, 2L, 3L, 4L, 5L,
			6L, 7L, 8L, 9L, 10L
	};

	@Async
	@Scheduled(fixedDelay = 10)
	public void runSimulation() {
		if (!isRunning) {
			return;
		}

		try {
			OrderRequestDTO randomOrder = createRandomOrder();

			log.info(">>>> [BOT] 주문 생성 | 타입: {} | 사이드: {} | 가격: {} | 수량: {} | 총 주문액: {}",
				randomOrder.getOrderType(),
				randomOrder.getOrderSide(),
				randomOrder.getOrderPrice(),
				randomOrder.getOrderVolume(),
				randomOrder.getTotalPrice());

			orderService.placeOrder(randomOrder);

		} catch (Exception e) {
			log.error(">>>> [BOT] 주문 처리 중 오류: ", e);
		}
	}

	private OrderRequestDTO createRandomOrder() {
		var random = java.util.concurrent.ThreadLocalRandom.current();

		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getPrefix() + "market:info");
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
}
