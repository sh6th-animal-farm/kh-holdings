package com.kanghwang.khholdings.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {
	private String message;
	private T payload;

	// 데이터 없이 메시지만 보내는 응답
	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(message, null);
	}

	// 데이터를 포함한 응답
	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(message, data);
	}
}
