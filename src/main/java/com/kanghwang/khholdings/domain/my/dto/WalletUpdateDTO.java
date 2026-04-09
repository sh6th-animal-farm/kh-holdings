package com.kanghwang.khholdings.domain.my.dto;

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
public class WalletUpdateDTO {
	Long walletId;
	Long tokenId;
	String tokenName;
	String tickerSymbol;
	BigDecimal cashBalance;         // 총 예수금
	BigDecimal totalPurchasedValue; // 총 매입금액
	BigDecimal tokenQty;            // 해당 토큰의 총 수량
	BigDecimal tokenPurchasedVal;   // 해당 토큰의 총 매입금액
}
