package com.kanghwang.khholdings.domain.order.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanghwang.khholdings.domain.order.OrderRepository;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;

@Service
public class OrderDBService {

	@Autowired
	OrderRepository orderRepository;

	public BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId){
		return orderRepository.selectHoldingTokenBalance(walletId, tokenId);
	}

	public BigDecimal selectAvailableBalance(Long walletId){
		return orderRepository.selectAvailableBalance(walletId);
	}

	@Transactional
	public void placeOrder(OrderRequestDTO orderDto) {
		orderRepository.callPlaceOrderProcedure(orderDto);
	}
}
