package com.kanghwang.khholdings.domain.project.dto;

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
	private Long passPrice;  // 당첨 가격
	private Long passVolume; // 당첨 수량
}
