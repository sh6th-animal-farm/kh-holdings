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
public class SnapshotDTO {
    private Long userId;				// 유저 아이디
    private Long walletId;			    // 지갑 번호
    private Long tokenId;		        // 토큰 번호
    private BigDecimal tokenBalance;    // 토큰 보유 수량
}
