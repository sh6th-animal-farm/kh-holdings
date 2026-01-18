//package com.kanghwang.khholdings.global.errr;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.dao.DataAccessException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//@Slf4j
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(DataAccessException.class)
//    public ResponseEntity<String> handleDatabaseException(org.springframework.dao.DataAccessException e) {
//
//        log.error("DB 프로시저 실행 에러 발생: ", e);
//
//        // 프로시저의 RAISE EXCEPTION 메시지는 보통 e.getRootCause().getMessage()에 들어있습니다.
//        String fullMessage = e.getRootCause() != null ? e.getRootCause().getMessage() : e.getMessage();
//        String message = "데이터베이스 오류가 발생했습니다.";
//
//        if (fullMessage != null) {
//            message = fullMessage;
//        }
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
//    }
//}
