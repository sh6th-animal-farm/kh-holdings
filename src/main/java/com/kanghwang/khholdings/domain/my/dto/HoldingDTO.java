package com.kanghwang.khholdings.domain.my.dto;

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
public class HoldingDTO {
	private String tokenName;
	private String tickerSymbol;
	private BigDecimal tokenBalance;
	private BigDecimal purchasedValue;
	private BigDecimal marketValue;
	private BigDecimal profitLoss;
	private BigDecimal profitLossRate;
}
