package com.kanghwang.khholdings.domain.market.dto;

import java.math.BigDecimal;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderbookDTO {
	private BigDecimal price;         // 주문 가격
	private BigDecimal updatedVolume; // 해당 가격의 총 수량
	private OrderSide side;           // BUY or SELL
	private String action;            // UPDATE(수량 변경/추가) or DELETE(수량 삭제)
}
