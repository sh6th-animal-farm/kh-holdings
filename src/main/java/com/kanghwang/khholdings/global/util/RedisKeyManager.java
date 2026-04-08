package com.kanghwang.khholdings.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

@Component
public class RedisKeyManager {

    // 배포 서버(설정 없음) ➔ 기본값 "" (빈 문자열) 사용
    // 내 로컬(active profile: local) ➔ 설정값 "local" 사용
    @Value("${app.env:}")
    private String env;

    public String getPrefix() {
        return env == null || env.isEmpty() ? "" : env + ":";
    }

    // 1. 호가창 (ZSET): 레디스 체결 엔진 내 매칭 용
    public String getOrderBookKey(Long tokenId, OrderSide side) {
        return getPrefix() + "spot:orderbook:" + side.name().toLowerCase() + ":" + tokenId;
    }

    // 2. 토큰별 주문 상세 (HASH): 특정 코인 매칭 시 빠른 조회용
    public String getOrderInfoKey(Long tokenId) {
        return getPrefix() + "spot:orderbook:info:" + tokenId;
    }

    // 3. 체결 이벤트 스트림 (STREAM): 체결 결과를 DB에 비동기로 보낼 때 사용
    public String getTradeStreamKey() {
        return getPrefix() + "trade:stream:";
    }

    // 4. 차트/틱 소식 (TOPIC) : 단위별 봉 데이터 실시간 전파용
    public String getCandleTopicKey(Long tokenId, int unit) {
        return getPrefix() + "candle:topic:" + tokenId + ":" + unit;
    }

    // 5. 호가창 (MAP): 웹소켓을 통해 호가 정보를 브라우저에 보내줄 때 사용
    public String getOrderBookAggrKey(Long tokenId, OrderSide side) {
        return getPrefix() + "orderbook:aggr:" + tokenId + ":" + side.name().toLowerCase();
    }

    // 6. 체결창 (TOPIC): 웹소켓을 통해 체결 정보를 브라우저에 보내줄 때 사용
    public String getTradeTopicKey(Long tokenId) {
        return getPrefix() + "trade:topic:" + tokenId;
    }

    // 7. 종목 요약 (MAP): 토큰 실시간 리스트에 표시할 정보를 저장
    public String getMarketInfoKey() {
        return getPrefix() + "market:info";
    }

    // 8. 종목 순위 (ZSET): 토큰 실시간 리스트를 거래대금순으로 정렬하기 위해 사용
    public String getMarketRankKey() {
        return getPrefix() + "market:ranking";
    }

    // 9. 종목 정보 (TOPIC): 웹소켓을 통해 토큰 정보를 브라우저에 보내줄 때 사용
    public String getMarketUpdateTopicKey() {
        return getPrefix() + "market:update:topic";
    }

    // 10. 캔들 정보 (MAP)
    public String getCandleKey(Long tokenId, int unit, Long timestamp) {
        return getPrefix() + "candle:" + unit + "m:" + tokenId + ":" + timestamp;
    }

    // 11. 취소 주문 정보 (STRING)
    public String getCancelKey(Long orderId) {
        return getPrefix() + "cancel:mark:" + orderId;
    }

    // 12. 사용자별 주문 정보 (HASH)
    public String getPersonalOrderBookKey(Long walletId, OrderSide side) {
        return getPrefix() + "orderbook:" + walletId + ":" + side.name().toLowerCase();
    }
}
