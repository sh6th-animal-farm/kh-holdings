package com.kanghwang.khholdings.global.util;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
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
	public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message) {
		return ResponseEntity
			.status(status)
			.body(new ApiResponse<>(message, null));
	}
}
