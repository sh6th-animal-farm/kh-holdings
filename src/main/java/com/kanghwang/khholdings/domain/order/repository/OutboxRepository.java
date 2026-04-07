package com.kanghwang.khholdings.domain.order.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.order.dto.OrderOutboxDTO;

@Mapper
public interface OutboxRepository {

	// outbox 저장
	void insertOutbox(Long orderId, String type, String payload, String status);

	// outbox 상태 변경
	void updateOutboxStatus(Long orderId, String type, String status, int retryCount);

	// outbox 상태가 'PENDING'인 경우 변경
	int updateStatusIfPending(Long orderId, String type, String status);

	// outbox 상태를 'CANCELLED'로 변경
	int updateStatusToCancelled(Long orderId);

	// 상태가 'PENDING'이면서 가장 오래된 outbox 10개 조회
	List<OrderOutboxDTO> findPendingOrdersForUpdate();
}
