package com.kanghwang.khholdings.domain.market;

import org.redisson.api.RPatternTopic;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.kanghwang.khholdings.domain.market.dto.OrderbookDTO;
import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.TradeDTO;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
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

		tradeTopic.addListener(TradeDTO.class, (pattern, channel, event) -> {
			// 2. 체결 발생 시, 채널명에서 토큰 ID 추출
			// 채널 예: "trade:topic:777777"
			String[] parts = channel.toString().split(":");
			String tokenId = parts[parts.length - 1];

			// 3. STOMP 브로커를 통해 구독 중인 사용자들에게 전송
			// 프론트 구독 주소 예: /topic/trades/777777
			messagingTemplate.convertAndSend("/topic/trades/" + tokenId, event);

			System.out.println("[MarketWorker] WebSocket 체결: " + event.getTakerSide() + " " + tokenId + " 가격 " + event.getPrice() + ", 수량 " + event.getVolume());
		});

		// [주문/호가]
		RPatternTopic orderTopic = redissonClient.getPatternTopic(redisKeyManager.getPrefix() + "orderbook:aggr:*:*");

		orderTopic.addListener(OrderbookDTO.class, (pattern, channel, event) -> {
			// channel 형태: "kh:orderbook:aggr:777:buy"
			String[] parts = channel.toString().split(":");
			String tokenId = parts[parts.length - 2];
			String side = parts[parts.length - 1];

			messagingTemplate.convertAndSend("/topic/orders/" + tokenId, event);

			System.out.println("[MarketWorker] WebSocket 호가: " + event.getSide() + " " + tokenId + " 가격 " + event.getPrice() + ", 수량 " + event.getUpdatedVolume() + " (" + event.getAction() + ")");
		});

		// [차트]
		RPatternTopic candleTopic = redissonClient.getPatternTopic(redisKeyManager.getPrefix() + "candle:topic:*");

		// 리스너 타입을 candleDTO로 명시
		candleTopic.addListener(CandleDTO.class, (pattern, channel, event) -> {
			String[] parts = channel.toString().split(":");
			String tokenId = parts[parts.length - 1];

			messagingTemplate.convertAndSend("/topic/candles/" + tokenId, event);

			System.out.println("[MarketWorker] WebSocket : OHLCV 및 차트 업데이트 토큰 id: " + event.getTokenId() + ", 시가: " + event.getOpeningPrice()
					+ ", 고가: " + event.getHighPrice() + " 저가: " + event.getLowPrice()
					+ ", 종가: " + event.getClosingPrice()
					+ ", 거래량: " + event.getTradeVolume()
					+ ", 캔들 시간: " + event.getCandleTime());

		});

		// [전체 토큰 리스트]
		RTopic tokenListTopic = redissonClient.getTopic(redisKeyManager.getPrefix() + "market:update:topic");

		tokenListTopic.addListener(TokenListDTO.class, (channel, event) -> {
			messagingTemplate.convertAndSend("/topic/tokenList", event);

			System.out.println("[MarketWorker] WebSocket 전광판 업데이트: " + event.getTickerSymbol()
				+ " 현재가: " + event.getMarketPrice() + " 등락률: " + event.getChangeRate() + "%");
		});
	}
}
