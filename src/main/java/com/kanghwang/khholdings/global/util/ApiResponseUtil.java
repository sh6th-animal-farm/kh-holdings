package com.kanghwang.khholdings.global.util;

import java.util.List;
import java.util.Collections;

import org.springframework.http.ResponseEntity;

import com.kanghwang.khholdings.global.dto.ApiResponse;

public class ApiResponseUtil {

	// 성공, 데이터 O
	public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
		return ResponseEntity.ok(new ApiResponse(message, data));
	}

	// 성공, 데이터 X
	public static <T> ResponseEntity<ApiResponse<List<T>>> ok(String message) {
		return ResponseEntity.ok(new ApiResponse(message, Collections.emptyList()));
	}

	// 실패
	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(message, null);
	}
}
