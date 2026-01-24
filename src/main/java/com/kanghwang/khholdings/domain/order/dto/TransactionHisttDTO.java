package com.kanghwang.khholdings.domain.order.dto;

import com.kanghwang.khholdings.domain.order.type.OrderSide;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionHisttDTO {

	private Long transactionId; 			// p_tx_id
	private Long tradeId;
	private Long orderId;
	private Long walletId;
	private String transactionType; 		// 'TRADE'
	private String assetType; 				// 'CASH', 'TOKEN'
	private String direction; 				// 'IN', 'OUT'
	private BigDecimal amount; 				// 체결 금액 또는 수량
	private BigDecimal balanceAfter;
	private BigDecimal remainingVolume;
	private BigDecimal remainingPrice;
	private BigDecimal fee;
	private String hashValue;
	private OffsetDateTime createdAt;
}
