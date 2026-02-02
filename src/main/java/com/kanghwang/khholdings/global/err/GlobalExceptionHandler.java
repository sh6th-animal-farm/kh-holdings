package com.kanghwang.khholdings.global.err;

import org.redisson.client.RedisException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kanghwang.khholdings.global.dto.ApiResponse;
import com.kanghwang.khholdings.global.util.ApiResponseUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. DB 관련 에러 캐치
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseError(DataAccessException e) {
        log.error("데이터베이스 오류 발생: {}", e.getMessage());
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "데이터 처리 중 문제가 발생했습니다.";
        }
        return ResponseEntity.status(500).body(ApiResponseUtil.error(message));
    }

    // 2. 그 외 모든 일반 에러 캐치
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralError(Exception e) {
        log.error("서버 오류 발생: {}", e.getMessage());
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "서버 내부 오류가 발생했습니다.";
        }

        return ResponseEntity.status(500).body(ApiResponseUtil.error(message));
    }

    // 3. Redis 관련 에러 캐치
    @ExceptionHandler(RedisException.class)
    public ResponseEntity<ApiResponse<Void>> handleRedisError(RedisException e) {
        log.error("레디스 작업 중 오류 발생: {}", e.getMessage());
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "레디스 작업 중 문제가 발생했습니다.";
        }

        return ResponseEntity.status(500).body(ApiResponseUtil.error(message));
    }

    // 4. 잘못된 요청 에러 캐치
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("잘못된 요청 발생: {}", e.getMessage());
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            message = "잘못된 요청입니다.";
        }

        return ResponseEntity.status(400).body(ApiResponseUtil.error(message));
    }
}
