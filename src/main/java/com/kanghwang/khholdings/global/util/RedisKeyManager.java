package com.kanghwang.khholdings.global.util;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

public class RedisKeyManager {
    private static final String BASE_PATH = "spot:";

    // 1. 호가창 (ZSET): 가격순 정렬용
    public static String getOrderBookKey(Long tokenId, OrderSide side) {
        return BASE_PATH + "orderbook:" + side.name().toLowerCase() + ":" + tokenId;
    }

    // 2. 토큰별 주문 상세 (HASH): 특정 코인 매칭 시 빠른 조회용
    public static String getOrderInfoKey(Long tokenId) {
        return BASE_PATH + "order:info:" + tokenId;
    }

    // 체결 이벤트 스트림 (STREAM/TOPIC): 체결 결과를 DB에 비동기로 보낼 때 사용
    public static String getGlobalOrderInfoKey() {
        return BASE_PATH + "orders:all";
    }
}
