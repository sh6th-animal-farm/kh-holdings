package com.kanghwang.khholdings;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@EnableScheduling
@SpringBootApplication
public class KhHoldingsApplication {

	@PostConstruct
	public void started() {
		// 앱 전체의 기본 시간대를 서울로 고정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(KhHoldingsApplication.class, args);
	}

}
