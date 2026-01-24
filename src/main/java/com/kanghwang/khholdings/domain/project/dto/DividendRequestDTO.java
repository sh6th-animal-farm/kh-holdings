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
public class DividendRequestDTO {
	private Long dividendId;
	private Long walletId;
	private BigDecimal beforeTaxAmount; // 세전 배당금
	private BigDecimal afterTaxAmount;  // 새후 배당금
}
