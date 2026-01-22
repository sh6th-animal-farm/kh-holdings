package com.kanghwang.khholdings.domain.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

	// 우리 db에 있지 않나 있으면 그거 쓰는 걸로
	private LocalDateTime createdAt; 		// 체결 시각
	private String takerSide;       		// 체결을 유발한 쪽 (BUY 또는 SELL)
}
