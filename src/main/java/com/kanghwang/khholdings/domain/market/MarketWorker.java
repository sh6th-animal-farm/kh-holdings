package com.kanghwang.khholdings.domain.market;

import org.redisson.api.RPatternTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import com.kanghwang.khholdings.global.dto.RealTimeEvent;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MarketWorker {

	private final RedissonClient redissonClient;
	private final SimpMessagingTemplate messagingTemplate;

	@PostConstruct
	public void listenTradeTopic() {

		// Jackson이 OffsetDateTime을 읽을 있도록 JavaTimeModule 등록
		ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule()) // Java 8 날짜 타입 지원 추가
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// [체결]
		// 1. Redis Topic 구독 (패턴 매칭 사용: 모든 토큰의 체결을 감시)
		// JsonJacksonCodec을 사용하여 브라우저가 읽을 수 있는 JSON 형태로 받기
		RPatternTopic tradeTopic = redissonClient.getPatternTopic("trade:topic:*", new JsonJacksonCodec(objectMapper));

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
		RPatternTopic orderTopic = redissonClient.getPatternTopic("order:topic:*", new JsonJacksonCodec(objectMapper));

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
	}
}
