package com.kanghwang.khholdings.domain.project.dto;

import java.math.BigDecimal;

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
public class SubscriptionRequestDTO {
	private Long subscriptionId;
	private Long walletId;
	private BigDecimal passPrice;  // 당첨 가격
	private BigDecimal passVolume; // 당첨 수량
}
