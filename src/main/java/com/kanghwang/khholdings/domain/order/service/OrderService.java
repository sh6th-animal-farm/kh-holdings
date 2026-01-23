package com.kanghwang.khholdings.domain.order.service;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanghwang.khholdings.domain.order.OrderRepository;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	private final SnowflakeIdGenerator idGenerator;
	private final OrderDBService orderDBService;
	private final OrderRedisService orderRedisService;
	private final SnowflakeIdGenerator snowflakeIdGenerator;
	private final OrderRepository orderRepository;

	// 해당 토큰 보유 수량 조회
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

		// 1. Snowflake ID를 사용하여 주문 번호 생성
		Long orderId = snowflakeIdGenerator.nextId();
		orderDto.setOrderId(orderId);

		// 2. 주문 요청 시, 미체결 금액(수량)을 주문 금액(수량)으로 초기화
		if (orderDto.getOrderSide() == OrderSide.BUY) {
			orderDto.setRemainingCash(orderDto.getTotalPrice());
		} else {
			orderDto.setRemainingCash(BigDecimal.ZERO);
		}

		orderDto.setRemainingToken(orderDto.getOrderVolume());

		// 3. [FEAT] 서비스 단에서의 1차 잔액 검증
		if (orderDto.getOrderSide() == OrderSide.BUY) {
			BigDecimal available_cash = orderDBService.selectAvailableBalance(orderDto.getWalletId());
			if (available_cash.compareTo(orderDto.getTotalPrice()) < 0) {
				throw new RuntimeException("잔액이 부족합니다.");
			}
		} else {
			BigDecimal holdingToken = orderDBService.selectHoldingTokenBalance(orderDto.getWalletId(), orderDto.getTokenId());
			if (holdingToken.compareTo(orderDto.getOrderVolume()) < 0) {
				throw new RuntimeException("보유 수량이 부족합니다.");
			}
		}

		// 2. DB에서 잔액 차감 및 홀딩
		// 기존의 프로시저 그대로 사용
		orderDBService.placeOrder(orderDto);

		// 3. Redis에 동일한 요청
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				orderRedisService.processOrder(orderDto);
				log.info("[OrderService] DB 커밋 완료 후 Redis 엔진에 주문 추가: {}", orderDto.getOrderId());
			}
		});
	}

	// 주문 취소
	public boolean cancelOrder(Long tokenId, Long orderId) {
		return orderRedisService.cancelOrder(tokenId, orderId);
	}
}
