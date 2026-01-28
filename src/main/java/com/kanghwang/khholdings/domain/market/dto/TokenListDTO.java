package com.kanghwang.khholdings.domain.market.dto;

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
public class TokenListDTO {
	private Long tokenId;
	private String tokenName;
	private String tickerSymbol;
	private BigDecimal marketPrice;			// 현재가(시장가)
	private BigDecimal dailyTradeVolume;	// 오늘 누적 거래 대금
	
	private BigDecimal openPrice;			// 오늘 오전 9시 시작 가격
	private BigDecimal changeRate;			// 등락률
	
	// private BigDecimal highPrice;			
	// private BigDecimal lowPrice;

}
