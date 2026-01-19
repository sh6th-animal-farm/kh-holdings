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
public class TransactionRequestDTO {
	private Long txId1;
	private Long txId2;
	private Long txId3;
	private Long txId4;
	private Long tradeId;
	private Long buyOrderId;
	private Long sellOrderId;
	private BigDecimal targetPrice;
	private BigDecimal executedVolume;
	private BigDecimal feeRate;
}
