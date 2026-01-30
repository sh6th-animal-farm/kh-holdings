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
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResultDTO {
	private Long walletId;
	private Long passTxId; // 청약 당첨된 거래내역 번호 (증권사 체결 번호)
	private Long failTxId; // 환불 처리된 거래내역 번호 (증권사 체결 번호)
	private BigDecimal passVolume; // 지급된 토큰 개수
	private BigDecimal passAmount; // 토큰 개당 가격 * 지급된 토큰 개수
}
