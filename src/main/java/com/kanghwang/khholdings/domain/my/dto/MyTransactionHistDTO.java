package com.kanghwang.khholdings.domain.my.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyTransactionHistDTO {
    private OffsetDateTime createdAt;  // 거래 시각
    private String transactionType;    // 거래 유형(BUY, SELL, PASS, FAIL, DIVIDEND, BURN)
    private String tokenName;          // 토큰 이름
    private String tickerSymbol;       // 종목 코드
    private BigDecimal executedPrice;  // 체결 가격
    private BigDecimal executedVolume; // 체결 수량
    private BigDecimal executedAmount; // 체결 총액
    private BigDecimal balanceAfter;   // 체결 후 잔액
}
