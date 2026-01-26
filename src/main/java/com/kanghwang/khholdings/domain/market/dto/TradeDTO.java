package com.kanghwang.khholdings.domain.market.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeDTO {
	private BigDecimal price;         // 체결 가격
	private BigDecimal volume;        // 체결 수량
	private OrderSide takerSide;      // Taker의 주문 방향
	private OffsetDateTime createdAt; // 체결 시각
}
