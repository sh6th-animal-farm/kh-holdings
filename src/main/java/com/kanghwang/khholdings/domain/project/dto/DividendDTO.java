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
	private Long walletId;
	private BigDecimal amount;
	private BigDecimal fee;
	private String hashValue;
}
