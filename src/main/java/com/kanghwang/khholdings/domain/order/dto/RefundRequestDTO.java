package com.kanghwang.khholdings.domain.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;

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
public class RefundRequestDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long txId;
	private Long orderId;
	private BigDecimal remainingCash;
	private BigDecimal remainingToken;
}
