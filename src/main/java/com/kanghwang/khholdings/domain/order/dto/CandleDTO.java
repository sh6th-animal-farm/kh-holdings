package com.kanghwang.khholdings.domain.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleDTO {
    private Long tokenId;               // 토큰 고유 번호
    private Integer unit;               // unit (1분봉이면 1)
    private OffsetDateTime candleTime;  // 캔들 기준 시간

    private BigDecimal openingPrice;    // 시가
    private BigDecimal highPrice;       // 고가
    private BigDecimal lowPrice;        // 저가
    private BigDecimal closingPrice;    // 종가
    private BigDecimal tradeVolume;     // 거래량
}
