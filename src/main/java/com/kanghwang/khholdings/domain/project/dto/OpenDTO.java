package com.kanghwang.khholdings.domain.project.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
public class OpenDTO {
	private String tokenName;         // 토큰명
	private String tickerSymbol;      // 종목 코드
	private BigDecimal totalSupply;  // 총 발행량
	private BigDecimal issuePrice;    // 발행 단가
	private OffsetDateTime createdAt; // 발행 일시
}
