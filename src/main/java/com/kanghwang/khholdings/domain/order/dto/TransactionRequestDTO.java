package com.kanghwang.khholdings.domain.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long txId1;                     // 거래 내역 번호 - 매수자 현금 지출
	private Long txId2;                     // 거래 내역 번호 - 매수자 토큰 유입
	private Long txId3;                     // 거래 내역 번호 - 매도자 현금 유입
	private Long txId4;                     // 거래 내역 번호 - 매도자 토큰 지출
	private Long tradeId;                   // 체결 번호 (체결 쌍에 대해 동일한 번호 부여)
	private Long buyOrderId;                // 매수자 주문 번호
	private Long sellOrderId;               // 매도자 주문 번호
	private BigDecimal targetPrice;         // 체결 가격
	private BigDecimal executedVolume;      // 체결 수량
	private BigDecimal feeRate;             // 수수료율
	private OffsetDateTime createdAt; 		// 체결 시각
	private OrderSide takerSide;       		// 체결을 유발한 쪽 (BUY or SELL, 항상 나중에에 들어온 쪽(mySide))
	private Long tokenId;      				// 토큰 번호  (어떤 종목인지 식별)
	private Long buyWalletId;  				// 매수자 지갑 (체결 목록 저장 dto에 전달 위함)
	private Long sellWalletId; 				// 매도자 지갑 (체결 목록 저장 dto에 전달 위함)
}
