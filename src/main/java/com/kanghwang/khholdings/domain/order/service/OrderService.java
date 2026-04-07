package com.kanghwang.khholdings.domain.order.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanghwang.khholdings.domain.order.dto.CancelRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.repository.OrderRepository;
import com.kanghwang.khholdings.domain.order.repository.OutboxRepository;
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
	private final OutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

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

		// 3. DB 주문 생성 + Outbox 저장
		orderDBService.placeOrder(orderDTO);
		insertOutbox(orderDTO, "ORDER"); // PENDING

		// 4. DB에서 주문 생성 후, Redis 매칭 엔진에 추가
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				// PENDING인 경우에만 PROCESSING으로 변경
				// -> 리턴값이 1이면 내가 선점, 0이면 찰나의 순간에 스케줄러가 가져간 것
				int updated = outboxRepository.updateStatusIfPending(orderDTO.getOrderId(), "ORDER", "PROCESSING");

				if (updated == 1) {
					try{
						// Redis 전송
						processRedisWithStatus(orderDTO, "ORDER");

						// 성공 시 처리 완료 (PROCESSED)
						updateOutboxStatus(orderId, "ORDER", "PROCESSED", 0);
						log.info("[주문 - 실시간 처리 성공] ID: {}, count: 0", orderId);
					} catch (Exception e) {
						// 실패 시 대기 (PENDING)
						updateOutboxStatus(orderId, "ORDER", "PENDING", 0);
						log.error("[주문 - 실시간 처리 실패] ID: {}, count: 0", orderId);
						log.error("error: {}", e.getMessage());
					}
				} else {
					log.info("스케줄러가 이미 처리하고 있습니다. (ID: {}, type: ORDER)", orderDTO.getOrderId());
				}
			}
		});
	}

	// 주문 취소
	@Transactional
	public void cancelOrder(CancelRequestDTO cancelDTO) {

		// 1. 주문 outbox의 상태를 'CANCELLED'로 변경
		outboxRepository.updateStatusToCancelled(cancelDTO.getOrderId());

		// 2. 주문 취소를 outbox에 저장
		insertOutbox(cancelDTO, "CANCEL"); // PENDING

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				// 2. 선점 시도
				int updated = outboxRepository.updateStatusIfPending(cancelDTO.getOrderId(), "CANCEL", "PROCESSING");

				// 3. 상태가 'PENDING'인 경우 취소
				if (updated == 1) {
					Long orderId = cancelDTO.getOrderId();

					try {
						// Stream 전송
						processRedisWithStatus(cancelDTO, "CANCEL");

						// 성공 시 처리 완료 (PROCESSED)
						updateOutboxStatus(orderId, "CANCEL", "PROCESSED", 0);
						log.info("[취소 - 실시간 처리 성공] ID: {}, count: 0", orderId);
					} catch (Exception e) {
						// 실패 시 대기 (PENDING)
						updateOutboxStatus(orderId, "CANCEL", "PENDING", 0);
						log.error("[취소 - 실시간 처리 실패] ID: {}, count: 0", orderId);
						log.error("error: {}", e.getMessage());
					}
				} else {
					log.info("스케줄러가 이미 처리하고 있습니다. (ID: {}, type: CANCEL)", cancelDTO.getOrderId());
				}
			}
		});
	}

	// 요청 전송
	public void processRedisWithStatus(Object dto, String type) throws Exception {
		if ("ORDER".equals(type)) {
			OrderRequestDTO orderDto = (OrderRequestDTO) dto;
			orderRedisService.processOrder(orderDto);
		} else if ("CANCEL".equals(type)) {
			CancelRequestDTO cancelDto = (CancelRequestDTO) dto;
			orderRedisService.cancelOrder(cancelDto);
		}
	}

	// outbox 입력
	private void insertOutbox(Object dto, String type) {
		try {
			Long orderId = 0L;
			String json = "";

			if ("ORDER".equals(type)) {
				OrderRequestDTO orderDto = (OrderRequestDTO) dto;
				orderId = orderDto.getOrderId();
				json = objectMapper.writeValueAsString(orderDto);
			} else if ("CANCEL".equals(type)) {
				CancelRequestDTO cancelDto = (CancelRequestDTO) dto;
				orderId = cancelDto.getOrderId();
				json = objectMapper.writeValueAsString(cancelDto);
			}

			outboxRepository.insertOutbox(orderId, type, json, "PENDING");
			log.info("Outbox 저장 성공 (ID: {}, type: {})", orderId, type);
		} catch (Exception e) {
			throw new RuntimeException("Outbox 저장 실패", e);
		}
	}

	// 별도 트랜잭션으로 상태 즉시 업데이트
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateOutboxStatus(Long orderId, String type, String status, int retryCount) {
		outboxRepository.updateOutboxStatus(orderId, type, status, retryCount);
	}
}
