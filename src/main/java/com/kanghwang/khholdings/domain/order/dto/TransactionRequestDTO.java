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
	private Long tokenId;      				// 어떤 종목인지 식별
	private Long buyWalletId;  				// 매수자 지갑 (체결 목록 저장 dto에 전달 위함)
	private Long sellWalletId; 				// 매도자 지갑 (체결 목록 저장 dto에 전달 위함)
}
