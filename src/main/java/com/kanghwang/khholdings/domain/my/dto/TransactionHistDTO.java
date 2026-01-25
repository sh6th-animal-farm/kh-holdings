package com.kanghwang.khholdings.domain.my.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHistDTO {
    private Long transactionId;                 // 체결 내역 번호
    private Long tradeId;                       // 거래 번호
    private Long orderId;                       // 주문 번호
    private Long walletId;                      // 지갑 번호

    private String transactionType;             // "TRADE" 고정
    private String assetType;                   // "CASH" 또는 "TOKEN"
    private String direction;                   // "IN" 또는 "OUT"

    private BigDecimal amount;                  // 이번에 움직인 수량/금액
    private BigDecimal balanceAfter;            // 이거 처리하고 남은 최종 잔액
    private BigDecimal remainingVolume;         // 주문 후 남은 토큰 수량
    private BigDecimal remainingPrice;          // 주문 후 남은 현금 금액
    private BigDecimal fee;                     // 발생한 수수료

    private String hashValue;                   // 데이터 위변조 방지용 해시
    private OffsetDateTime createdAt;           // 체결 시각

}
