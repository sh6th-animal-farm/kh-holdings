package com.kanghwang.khholdings.domain.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

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
public class TransactionRequestDTO implements Serializable {

	private static final long serialVersionUID = 1L;

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
	private OffsetDateTime createdAt; 		// 체결 시각
	private OrderSide takerSide;       		// 체결을 유발한 쪽 (=나, BUY or SELL)
}
