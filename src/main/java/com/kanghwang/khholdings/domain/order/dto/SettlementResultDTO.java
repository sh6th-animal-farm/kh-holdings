package com.kanghwang.khholdings.domain.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SettlementResultDTO {
    private BigDecimal buyCashAfter;        // 매수자 체결 후 현금 잔액
    private BigDecimal buyTokenAfter;       // 매수자 체결 후 토큰 잔액
    private BigDecimal sellCashAfter;       // 매도자 체결 후 현금 잔액
    private BigDecimal sellTokenAfter;      // 매도자 체결 후 토큰 잔액
    private BigDecimal buyRemCash;          // 매수 주문 잔량 (현금)
    private BigDecimal buyRemToken;         // 매수 주문 잔량 (토큰)
    private BigDecimal sellRemCash;         // 매도 주문 잔량 (현금)
    private BigDecimal sellRemToken;        // 매도 주문 잔량 (토큰)
}
