package com.kanghwang.khholdings.domain.order.dto;

import java.math.BigDecimal;

import com.kanghwang.khholdings.domain.order.type.OrderState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfoDTO {
	private Long orderId;             // 주문 고유 번호
	private OrderState orderState;    // 주문 상태
	private BigDecimal orderVolume;   // 주문 수량
	private BigDecimal totalPrice;    // 총 주문 금액 (시장가)
}
