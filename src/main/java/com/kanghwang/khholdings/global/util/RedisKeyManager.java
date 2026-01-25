package com.kanghwang.khholdings.global.util;

import com.kanghwang.khholdings.domain.order.type.OrderSide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyManager {

    // 배포 서버(설정 없음) ➔ 기본값 "" (빈 문자열) 사용
    // 내 로컬(active profile: local) ➔ 설정값 "local" 사용
    @Value("${app.env:}")
    private String env;

    public String getPrefix() {
        return env == null || env.isEmpty() ? "" : env + ":";
    }

    // 1. 호가창 (ZSET): 가격순 정렬용
    public String getOrderBookKey(Long tokenId, OrderSide side) {
        return getPrefix() + "spot:orderbook:" + side.name().toLowerCase() + ":" + tokenId;
    }

    // 2. 토큰별 주문 상세 (HASH): 특정 코인 매칭 시 빠른 조회용
    public String getOrderInfoKey(Long tokenId) {
        return getPrefix() + "spot:orderbook:info:" + tokenId;
    }

    // 3. 체결 이벤트 스트림 (STREAM): 체결 결과를 DB에 비동기로 보낼 때 사용
    public String getTradeStreamKey() {
        return getPrefix() + "orders:all";
    }

    // 4. 차트/틱 소식 (TOPIC)
    public String getCandleTopicKey(Long tokenId) {
        return getPrefix() + "candle:topic:" + tokenId;
    }
}
