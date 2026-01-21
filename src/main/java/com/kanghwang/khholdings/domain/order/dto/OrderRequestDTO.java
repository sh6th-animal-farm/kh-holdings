package com.kanghwang.khholdings.domain.order.dto;

import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.domain.order.type.OrderType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class OrderRequestDTO {
	private Long orderId;             // order_id, 주문 고유 번호
	private Long walletId;            // wallet_id, 지갑 번호
	private Long tokenId;             // token_id, 토큰 고유 번호
	private OrderSide orderSide;      // order_side, 주문 방향(매수, 매도)
	private OrderType orderType;      // order_type, 주문 유형(시장가, 지정가)
	private BigDecimal orderPrice;    // order_price, 주문 단가
	private BigDecimal orderVolume;   // order_volume, 주문 수량
	private BigDecimal totalPrice;    // total_orice, 시장가 매수용 총 금액
	private BigDecimal remainingCash; // remaining_cash, 미체결 금액
	private BigDecimal remainingToken; // remaining_token, 미체결 수량
}
