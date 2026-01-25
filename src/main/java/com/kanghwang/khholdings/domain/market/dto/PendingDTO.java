package com.kanghwang.khholdings.domain.market.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.kanghwang.khholdings.domain.order.type.OrderSide;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingDTO {
	private Long orderId;
	private OrderSide orderSide;
	private BigDecimal orderPrice;
	private BigDecimal orderVolume;
	private BigDecimal remainingToken;
	private OffsetDateTime createdAt;
}
