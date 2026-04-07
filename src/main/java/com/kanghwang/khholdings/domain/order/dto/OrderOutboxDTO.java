package com.kanghwang.khholdings.domain.order.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderOutboxDTO {
	private Long orderId;    // 주문 번호 (Snowflake ID)
	private String type;     // 주문 타입 (ORDER, CANCEL)
	private String payload;  // 주문 또는 주문 취소 시 전달한 데이터를 텍스트로 변환한 값
	private String status;   // 주문 상태 (PENDING, PROCESSING, PROCESSED, FAILED)
	private int retryCount;  // 재시도 횟수 (0 ~ 5)
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
}
