package com.kanghwang.khholdings.domain.order.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final SnowflakeIdGenerator idGenerator;
	private final OrderDBService orderDBService;
	private final OrderRedisService orderRedisService;
	private final SnowflakeIdGenerator snowflakeIdGenerator;

	// 특정 토큰 보유 수량 조회
	public BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId){
		return orderDBService.selectHoldingTokenBalance(walletId, tokenId);
	}

	// 주문 가능 금액 조회
	public BigDecimal selectAvailableBalance(Long walletId){
		return orderDBService.selectAvailableBalance(walletId);
	}

	// 매수/매도 주문
	@Transactional
	public void placeOrder(OrderRequestDTO orderDto) {

		// 1. Snowflake ID 생성
		Long orderId = snowflakeIdGenerator.nextId();

		orderDto.setOrderId(orderId);
		if (orderDto.getOrderSide() == OrderSide.BUY) {
			orderDto.setRemainingCash(orderDto.getTotalPrice());
		}
		orderDto.setRemainingToken(orderDto.getOrderVolume());

		// 2. DB에 저장하라고 넘김
		orderDBService.placeOrder(orderDto);

		// 3. Redis에 동일한 요청
		orderRedisService.processOrder(orderDto);
	}
}
