package com.kanghwang.khholdings.global.util;

import org.springframework.stereotype.Component;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

@Component
public class RedisKeyManager {

	// [Redis] 데이터 키
    // 1. 호가창 (ZSET): 가격순 정렬용
    public String getOrderBookKey(Long tokenId, OrderSide side) {
        return "spot:orderbook:" + side.name().toLowerCase() + ":" + tokenId;
    }

    // 2. 토큰별 주문 상세 (HASH): 특정 코인 매칭 시 빠른 조회용
    public String getOrderInfoKey(Long tokenId) {
        return "spot:orderbook:info:" + tokenId;
    }

    // 3. 체결 이벤트 스트림 (STREAM): 체결 결과를 DB에 비동기로 보낼 때 사용
    public String getTradeStreamKey() {
		return "trade:stream:";
    }



	// [Redis] 발행/구독 채널 (Redis Topic)
	// 6. 체결창 (TOPIC): 웹소켓을 통해 체결 정보를 브라우저에 보내줄 때 사용
	public String getTradeTopicKey(Long tokenId) {
		return "trade:topic:" + tokenId;
	}

	//
	public String getOrderTopicKey(Long tokenId) {
		return "order:topic:" + tokenId;
	}

	// 3. 차트/틱 소식 (TOPIC)
	public String getCandleTopicKey(Long tokenId) {
		return "candle:topic:" + tokenId;
	}

	// 5. 호가창 (MAP): 웹소켓을 통해 호가 정보를 브라우저에 보내줄 때 사용
	public String getOrderBookAggrKey(Long tokenId, OrderSide side) {
		return "orderbook:aggr:" + tokenId + ":" + side.name().toLowerCase();
	}



	// [MarketWorker] 패턴 매칭용
	// 1.
	public String getTradeTopicPattern() {
		return "trade:topic:";
	}

	// 2.
	public String getOrderTopicPattern() {
		return "order:topic:";
	}

	// 3.
	public String getCandleTopicPattern() {
		return "candle:topic:";
	}



	// [Websocket/STOMP] 웹소켓 목적지
	// 프론트엔드에서 사용
	public String getWsTradeDest(String tokenId) {
		return "/topic/trades/" + tokenId;
	}

	public String getWsOrderDest(String tokenId) {
		return "/topic/orders/" + tokenId;
	}

	public String getWsCandleDest(String tokenId) {
		return "/topic/candles/" + tokenId;
	}





	//
	public String getLastPriceKey(Long tokenId) {
		return "ticker:last_price:" + tokenId;
	}
}
