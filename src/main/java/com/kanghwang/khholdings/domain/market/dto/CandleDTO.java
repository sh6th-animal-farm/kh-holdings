package com.kanghwang.khholdings.domain.market.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleDTO implements Serializable {
    private Long tokenId;               // 토큰 고유 번호
    private int unit;                   // unit (1분봉이면 1)
    private Long candleTime;            // 캔들 기준 시간, TrainingView 연동을 위해 Long 사용
    private BigDecimal openingPrice;    // 시가
    private BigDecimal highPrice;       // 고가
    private BigDecimal lowPrice;        // 저가
    private BigDecimal closingPrice;    // 종가
    private BigDecimal tradeVolume;     // 거래량
    private BigDecimal tradeAmount;     // 거래대금
}
