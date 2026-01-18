package com.kanghwang.khholdings.domain.order.dto;

import com.kanghwang.khholdings.domain.order.type.OrderSide;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisOrderDTO implements Serializable {
    private String orderId;
    private Long walletId;
    private Long tokenId;
    private BigDecimal price;
    private BigDecimal volume;
    private BigDecimal remainingCash;
    private OrderSide side;
}
