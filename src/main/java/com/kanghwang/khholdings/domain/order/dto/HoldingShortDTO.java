package com.kanghwang.khholdings.domain.order.dto;

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
public class HoldingShortDTO {
	String tokenName;
	String tickerSymbol;
	BigDecimal quantity;
	BigDecimal purchasedValue;
}
