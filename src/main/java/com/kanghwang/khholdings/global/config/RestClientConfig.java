package com.kanghwang.khholdings.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

	// 외부 서버로부터 데이터 받아오기 위함
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
