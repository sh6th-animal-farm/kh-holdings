package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;

@Service
public class OrderService {

	@Autowired
	OrderDBService orderDBService;

	@Autowired
	OrderRedisService orderRedisService;

	public BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId){
		return orderDBService.selectHoldingTokenBalance(walletId, tokenId);
	}

	public BigDecimal selectAvailableBalance(Long walletId){
		return orderDBService.selectAvailableBalance(walletId);
	}

	@Transactional // DB 처리가 실패하면 전체를 취소함
	public void orderByCondition(OrderRequestDTO orDTO) {
		// 1. DB: 주문 저장 및 자산 동결 (가장 중요!)
		orderDBService.orderByCondition(orDTO);

		// 2. Redis: 실시간 매칭 엔진 가동
		// DB 작업이 무사히 끝난 직후에만 실행됩니다.
		// orderRedisService.processMatching(orDTO);
	}
}
