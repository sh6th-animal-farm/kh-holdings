package com.kanghwang.khholdings.domain.my.type;

import lombok.Getter;

@Getter
public enum TransactionHistType {
    TRADE("토큰 매매"),
    SUBSCRIPTION("청약"),
    DIVIDEND("배당"),
    DEPOSIT("입금"),
    WITHDRAW("출금"),
    CANCELLED("취소"),
    REFUND("환불");

    private final String description;

    TransactionHistType(String description) {
        this.description = description;
    }

    public static String getDisplayType(TransactionHistType type, String orderSide) {
        if (type == TRADE) {
            return "BUY".equalsIgnoreCase(orderSide) ? "매수" : "매도";
        }
        return type.getDescription();
    }

    // 이렇게 쓰는 게 맞나...............?
}
