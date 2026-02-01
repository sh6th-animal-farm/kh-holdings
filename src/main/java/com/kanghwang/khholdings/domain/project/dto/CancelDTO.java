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
public class CancelDTO {
	private Long transactionId;
	private Long walletId;
	private BigDecimal amount;
}
