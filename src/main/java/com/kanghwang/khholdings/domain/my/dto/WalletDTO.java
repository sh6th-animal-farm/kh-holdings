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
public class WalletDTO {
	private Long walletId;
	private Long userId;
	private String bankName;
	private String accountNo;
	private BigDecimal cashBalance;
	private BigDecimal totalPurchasedValue;
	private BigDecimal totalMarketValue;
	private BigDecimal totalBalance;
	private BigDecimal profitLoss;
	private BigDecimal profitLossRate;
}
