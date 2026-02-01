package com.kanghwang.khholdings.domain.order.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.kanghwang.khholdings.domain.order.OrderRepository;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import com.kanghwang.khholdings.domain.order.type.OrderType;
import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
	public void placeOrder(OrderRequestDTO orderDTO) {

		// 1. Snowflake ID를 사용하여 주문 번호 생성
		Long orderId = snowflakeIdGenerator.nextId();
		orderDTO.setOrderId(orderId);

		// 2. 주문 요청 시, 미체결 금액 및 미체결 수량 초기화
		// 1) 미체결 금액: 매수(BUY)는 총 주문 금액으로, 매도(SELL)은 0으로 초기화
		if (orderDTO.getOrderSide() == OrderSide.BUY) {
			orderDTO.setRemainingCash(orderDTO.getTotalPrice());
		} else {
			orderDTO.setRemainingCash(BigDecimal.ZERO);
		}

		// 2) 미체결 수량: 시장가 매수(MARKET, BUY)는 0으로, 그 외는 총 주문 수량으로 초기화
		if (orderDTO.getOrderType() == OrderType.MARKET && orderDTO.getOrderSide() == OrderSide.BUY) {
			orderDTO.setRemainingToken(BigDecimal.ZERO);
		} else {
			orderDTO.setRemainingToken(orderDTO.getOrderVolume());
		}

		// 3. DB에서 최소 주문 금액(수량) 확인, 자산 검증 및 동결, 주문 생성
		orderDBService.placeOrder(orderDTO);

		// 4. DB에서 주문 생성 후, Redis 매칭 엔진에 추가
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				log.info("매칭 엔진에 주문 추가: {}", orderDTO.getOrderId());
				orderRedisService.processOrder(orderDTO);
			}
		});
	}

	// 주문 취소
	public void cancelOrder(Long tokenId, Long orderId) {
		orderRedisService.cancelOrder(tokenId, orderId);
	}
}
