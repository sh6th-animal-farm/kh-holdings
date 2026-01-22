package com.kanghwang.khholdings.domain.order.bot;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

	// 봇의 실행 상태를 저장
	private volatile boolean isRunning = false;

	// 0.3초마다 주문
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
			log.error(">>>> [BOT] 주문 처리 중 오류: {}", e.getMessage());
		}
	}

	private OrderRequestDTO createRandomOrder() {
		var random = java.util.concurrent.ThreadLocalRandom.current();

		// 테스트용 데이터
		Long[] testWalletIds = {111111L, 222222L, 333333L, 444444L, 555555L};
		Long walletId = testWalletIds[random.nextInt(testWalletIds.length)];
		Long tokenId = 777777L;

		OrderSide side = random.nextBoolean() ? OrderSide.BUY : OrderSide.SELL;
		// 지정가 주문 80 : 시장가 주문 20
		OrderType type = (random.nextDouble() < 0.8) ? OrderType.LIMIT : OrderType.MARKET;

		BigDecimal price = BigDecimal.valueOf(100000 + (random.nextInt(5000)))
			.setScale(0, RoundingMode.FLOOR);

		BigDecimal volume = BigDecimal.valueOf(0.01 + (random.nextDouble() * 0.99))
			.setScale(4, RoundingMode.DOWN);

		var builder = OrderRequestDTO.builder()
			.walletId(walletId)
			.tokenId(tokenId)
			.orderSide(side)
			.orderType(type);

		if (type == OrderType.MARKET) {
			if (side == OrderSide.BUY) {
				builder.orderPrice(BigDecimal.ZERO);
				builder.orderVolume(BigDecimal.ZERO);
				builder.totalPrice(price.multiply(volume));
			} else {
				builder.orderPrice(BigDecimal.ZERO);
				builder.orderVolume(volume);
				builder.totalPrice(BigDecimal.ZERO);
			}
		} else {
			if (side == OrderSide.BUY) {
				builder.orderPrice(price);
				builder.orderVolume(volume);
				builder.totalPrice(price.multiply(volume));
			} else {
				builder.orderPrice(price);
				builder.orderVolume(volume);
				builder.totalPrice(BigDecimal.ZERO);
			}
		}

		return builder.build();
	}
}
