package com.kanghwang.khholdings.domain.my.dto;

<<<<<<< HEAD
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
=======
import com.kanghwang.khholdings.domain.my.type.TransactionHistType;
import lombok.*;

import java.math.BigDecimal;
>>>>>>> 537b31cc605d0dba29d706d05b098c2889d7e8d5

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistDTO {
<<<<<<< HEAD
    private OffsetDateTime createdAt;           // 거래 일시
    private String transactionType;    // 구분 [TODO] enum 타입 지정
=======
    private String createdAt;           // 거래 일시
    private TransactionHistType transactionType;    // 구분
>>>>>>> 537b31cc605d0dba29d706d05b098c2889d7e8d5
    private String tokenName;                   // 토큰명
    private Long tickerSymbol;                  // 종목 코드
    private BigDecimal unitPrice;               // 체결 단가
    private BigDecimal transactionVolume;       // 토큰 거래 수량
    private BigDecimal tokenBalanceAfter;       // 토큰 거래 후 수량
    private BigDecimal transactionAmount;       // 거래 금액
    private BigDecimal balanceAfter;            // 거래 후 잔액
}
