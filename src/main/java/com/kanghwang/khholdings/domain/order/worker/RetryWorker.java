package com.kanghwang.khholdings.domain.order.worker;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanghwang.khholdings.domain.order.dto.CancelRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderOutboxDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.repository.OutboxRepository;
import com.kanghwang.khholdings.domain.order.service.OrderService;
import com.kanghwang.khholdings.global.util.SlackAlarmUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryWorker {
	private final OutboxRepository outboxRepository;
	private final OrderService orderService;
	private final ObjectMapper objectMapper;
	private final SlackAlarmUtil slackAlarmUtil;

	@Scheduled(fixedDelay = 10000) // 10초
	public void retryFailedOrders() {
		// 1. PENDING 상태인 것들만 긁어옴 (SKIP LOCKED로 동시성 확보)
		List<OrderOutboxDTO> pendings = outboxRepository.findPendingOrdersForUpdate();

		for (OrderOutboxDTO outbox : pendings) {
			Long orderId = outbox.getOrderId();
			String type = outbox.getType(); // ORDER, CANCEL
			String typeKR = ("ORDER").equals(type) ? "주문" : "취소";
			int curRetry = outbox.getRetryCount();
			int nextRetry = curRetry + 1;
			String nextStatus = (nextRetry >= 5) ? "FAILED" : "PENDING";

			try {
				// 2. 즉시 PROCESSING으로 변경하여 다른 스레드 접근 차단 (트랜잭션 분리 권장)
				orderService.updateOutboxStatus(orderId, type, "PROCESSING", curRetry);

				// 3. json을 DTO로 변환하여 재시도 준비
				Object dto = ("ORDER".equals(type))
					? objectMapper.readValue(outbox.getPayload(), OrderRequestDTO.class)   // 주문 (ORDER)
					: objectMapper.readValue(outbox.getPayload(), CancelRequestDTO.class); // 취소 (CANCEL)

				// 4. 공통 전송 메서드 호출
				orderService.processRedisWithStatus(dto, type);

				// 성공 시 처리 완료 (PROCESSED)
				orderService.updateOutboxStatus(orderId, type, "PROCESSED", curRetry);
				log.info("[{} - 재시도 성공] ID: {}, count: {}", typeKR, orderId, curRetry);
			} catch (Exception e) {
				// 실패 시 재시도 횟수에 따라 상태 변경 (FAILED or PENDING)
				// -> 이후 스케줄러가 잡아 처리
				orderService.updateOutboxStatus(orderId, type, nextStatus, nextRetry);
				log.error("[{} - 재시도 실패] ID: {}, count: {}", typeKR, orderId, curRetry);
				log.error("error: {}", e.getMessage());

				// 최종 실패 시, 슬랙 알람 전송
				if ("FAILED".equals(nextStatus)) {
					sendSlackAlarm(typeKR, orderId, nextRetry, e.getMessage());
				}
			}
		}
	}

	private void sendSlackAlarm(String typeKR, Long orderId, int retryCount, String errorMessage) {
		StringBuilder sb = new StringBuilder();
		sb.append("🚨 [KH Holdings] 재시도 최종 실패 알림 🚨\n");
		sb.append("------------------------------------\n");
		sb.append(String.format("• 타입 : %s\n", typeKR));
		sb.append(String.format("• ID  : %d\n", orderId));
		sb.append("------------------------------------\n");
		sb.append("⚠️ 즉시 DB 확인 및 수동 조치가 필요합니다. ⚠️");

		slackAlarmUtil.sendAlarm(sb.toString());
	}
}