package com.kanghwang.khholdings.domain.project.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurnDTO {
    private Long txId1;                 // 트랜잭션 ID 1 (토큰 차감)
    private Long txId2;                 // 트랜잭션 ID 2 (현금 지급)
    private Long tradeId;               // 소각 번호 (토큰 차감과 현금 지급에 동일한 번호 부여)
    private Long tokenId;               // 토근 번호
    private Long walletId;              // 지갑 번호
    private String hashValue1;          // 해시 1
    private String hashValue2;          // 해시 2
    private BigDecimal amount;          // 소각 수량
    private BigDecimal cashAmount;      // 소각 수량 * 토큰 현재 가격

}
