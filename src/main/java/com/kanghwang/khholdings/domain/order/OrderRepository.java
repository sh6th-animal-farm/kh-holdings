package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;

@Mapper
public interface OrderRepository {

	public BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId);

	public BigDecimal selectAvailableBalance(Long walletId);

	void orderByCondition(OrderRequestDTO orDTO);

}
