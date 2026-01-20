package com.kanghwang.khholdings.domain.my.dto;

import com.kanghwang.khholdings.domain.my.type.TransactionHistType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistDTO {
    private String createdAt;           // 거래 일시
    private TransactionHistType transactionType;    // 구분
    private String tokenName;                   // 토큰명
    private Long tickerSymbol;                  // 종목 코드
    private BigDecimal unitPrice;               // 체결 단가
    private BigDecimal transactionVolume;       // 토큰 거래 수량
    private BigDecimal tokenBalanceAfter;       // 토큰 거래 후 수량
    private BigDecimal transactionAmount;       // 거래 금액
    private BigDecimal balanceAfter;            // 거래 후 잔액
}
