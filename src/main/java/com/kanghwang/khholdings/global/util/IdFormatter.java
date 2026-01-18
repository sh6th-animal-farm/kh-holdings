package com.kanghwang.khholdings.global.util;

public class IdFormatter {

    private static final String ORDER_PREFIX = "ORD-";
    private static final String TRADE_PREFIX = "TRD-";
    private static final String TRANSACTION_PREFIX = "TXN-";

    public static String formatOrderId(long id) {
        return ORDER_PREFIX + id;
    }

    public static String formatTradeId(long id) {
        return TRADE_PREFIX + id;
    }

    public static String formatTransactionId(long id) {
        return TRANSACTION_PREFIX;
    }

    public static long parseOrderId(String preId) {
        if (preId == null || !preId.startsWith(ORDER_PREFIX)) {
            throw new IllegalArgumentException("올바르지 않은 Order ID 형식입니다.");
        }
        return Long.parseLong(preId.replace(ORDER_PREFIX, ""));
    }

    public static long parseTradeId(String preId) {
        if (preId == null || !preId.startsWith(TRADE_PREFIX)) {
            throw new IllegalArgumentException("올바르지 않은 Trade ID 형식입니다.");
        }
        return Long.parseLong(preId.replace(TRADE_PREFIX, ""));
    }

    public static long parseTransactioId(String preId) {
        if (preId == null || !preId.startsWith(TRANSACTION_PREFIX)) {
            throw new IllegalArgumentException("올바르지 않은 Transation ID 형식입니다.");
        }
        return Long.parseLong(preId.replace(TRANSACTION_PREFIX, ""));
    }
}
