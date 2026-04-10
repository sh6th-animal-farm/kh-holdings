package com.kanghwang.khholdings.domain.order.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LiveMarketPriceDTO {
	Long tokenId;
	BigDecimal currentPrice;
}
