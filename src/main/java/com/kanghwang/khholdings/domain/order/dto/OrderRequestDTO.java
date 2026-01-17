package com.kanghwang.khholdings.domain.order.dto;

import java.math.BigDecimal;

import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.domain.order.type.OrderType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequestDTO {
	private Long orderId;           // order_id, 주문 고유 번호
	private Long walletId;          // wallet_id, 지갑 번호
	private Long tokenId;           // token_id, 토큰 고유 번호
	private OrderSide orderSide;       // order_side, 주문 방향(매수, 매도)
	private OrderType orderType;       // order_type, 주문 유형(시장가, 지정가)
	private BigDecimal orderPrice;  // order_price, 주문 단가
	private BigDecimal orderVolume; // order_volume, 주문 수량

	// 프로시저 실행에 필요한 추가 파라미터
	private BigDecimal totalCash;   // 시장가 매수용 총 금액
	private BigDecimal feeRate;     // 수수료율 (0.0006)
}
