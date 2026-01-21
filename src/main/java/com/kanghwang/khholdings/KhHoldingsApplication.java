package com.kanghwang.khholdings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class KhHoldingsApplication {

	public static void main(String[] args) {
		SpringApplication.run(KhHoldingsApplication.class, args);
	}

}
