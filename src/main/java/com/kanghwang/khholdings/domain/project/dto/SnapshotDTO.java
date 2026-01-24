package com.kanghwang.khholdings.domain.project.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotDTO {
    private Long userId;				// 유저 아이디
    private Long walletId;			    // 지갑 번호
    private Long tokenId;		        // 토큰 번호
    private BigDecimal tokenBalance;   // 보유 토큰 수량
}