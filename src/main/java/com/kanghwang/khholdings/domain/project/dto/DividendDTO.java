package com.kanghwang.khholdings.domain.project.dto;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DividendDTO {
	private Long transactionId;
	private Long dividendId;
	private Long tokenId;
	private Long walletId;
	private BigDecimal amount; // 세전 배당금
	private BigDecimal fee;    // 세후 배당금 - 세전 배당금
	private String hashValue;
}
