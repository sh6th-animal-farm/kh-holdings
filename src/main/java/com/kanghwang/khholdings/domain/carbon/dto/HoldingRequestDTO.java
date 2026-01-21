package com.kanghwang.khholdings.domain.carbon.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoldingRequestDTO {
	private Long tokenId;
	private BigDecimal myBalance;
	private BigDecimal enterpriseTotal;
}
