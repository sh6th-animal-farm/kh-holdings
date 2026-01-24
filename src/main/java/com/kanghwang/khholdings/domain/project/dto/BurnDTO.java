package com.kanghwang.khholdings.domain.project.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurnDTO {
    private Long txId1;                 // 트랜잭션 ID 1 (토큰 차감)
    private Long txId2;                 // 트랜잭션 ID 2 (현금 지급)
    private Long tokenId;               // 토근 번호
    private Long walletId;              // 지갑 번호
    private String hashValue1;          // 해시 1
    private String hashValue2;          // 해시 2
    private BigDecimal amount;          // 소각 수량
    private BigDecimal cashAmount;      // 소각 수량 * 토큰 현재 가격

}
