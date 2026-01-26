package com.kanghwang.khholdings.domain.market;

import org.redisson.api.RPatternTopic;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import com.kanghwang.khholdings.global.dto.RealTimeEvent;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MarketWorker {

	private final RedissonClient redissonClient;
	private final SimpMessagingTemplate messagingTemplate;
	private final RedisKeyManager redisKeyManager;

	@PostConstruct
	public void listenTradeTopic() {
		// [체결]
		// 1. Redis Topic 구독 (패턴 매칭 사용: 모든 토큰의 체결을 감시)
		// JsonJacksonCodec을 사용하여 브라우저가 읽을 수 있는 JSON 형태로 받기
		RPatternTopic tradeTopic = redissonClient.getPatternTopic(redisKeyManager.getPrefix() + "trade:topic:*");

		tradeTopic.addListener(TransactionRequestDTO.class, (pattern, channel, msg) -> {
			// 2. 체결 발생 시, 채널명에서 토큰 ID 추출
			// 채널 예: "trade:topic:777777"
			String tokenId = channel.toString().split(":")[2];

			// 3. STOMP 브로커를 통해 구독 중인 사용자들에게 전송
			// 프론트 구독 주소 예: /topic/trades/777777
			messagingTemplate.convertAndSend("/topic/trades/" + tokenId, msg);

			System.out.println("[MarketWorker] WebSocket 체결: TradeID " + msg.getTradeId());
		});

		// [주문/호가]
		RPatternTopic orderTopic = redissonClient.getPatternTopic(redisKeyManager.getPrefix() + "order:topic:*");

		orderTopic.addListener(RealTimeEvent.class, (pattern, channel, event) -> {

			String tokenId = channel.toString().split(":")[2];

			messagingTemplate.convertAndSend("/topic/orders/" + tokenId, event);

			String action = event.getAction();
			Object data = event.getData();

			// if ("INSERT".equals(action)) {
			// 	OrderRequestDTO orderDTO = (OrderRequestDTO) data;
			// 	System.out.println("[MarketWorker] WebSocket 주문 입력: OrderID " + orderDTO.getOrderId());
			// } else
			if ("UPDATE".equals(action)) {
				OrderRequestDTO orderDTO = (OrderRequestDTO) data;
				System.out.println("[MarketWorker] WebSocket 주문 등록: OrderID " + orderDTO.getOrderId());
			} else {
				System.out.println("[MarketWorker] WebSocket 주문 삭제: OrderID " + data);
			}
		});

		// [차트]
		RPatternTopic candleTopic = redissonClient.getPatternTopic(redisKeyManager.getPrefix() + "candle:topic:*");

		// 리스너 타입을 candleDTO로 명시
		candleTopic.addListener(CandleDTO.class, (pattern, channel, msg) -> {
			String[] parts = channel.toString().split(":");
			String tokenId = parts[parts.length - 1];

			messagingTemplate.convertAndSend("/topic/candles/" + tokenId, msg);

			System.out.println("[MarketWorker] WebSocket : OHLCV 및 차트 업데이트 " + msg);
		});
	}
}
