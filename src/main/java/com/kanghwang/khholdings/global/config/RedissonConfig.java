package com.kanghwang.khholdings.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RedissonConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private int port;

	@Value("${spring.data.redis.password}")
	private String password;

	@Value("${spring.data.redis.database:0}")
	private int database;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();

		// 1. Redis 연결 설정
		String redisAddress = String.format("redis://%s:%d", host, port);

		config.useSingleServer()
			.setAddress(redisAddress)
			.setPassword(password)
			.setDatabase(database);

		// 2. Jackson ObjectMapper 설정 (DTO 직렬화/역직렬화)
		// Jackson이 OffsetDateTime을 읽을 있도록 JavaTimeModule 등록
		ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule()) // Java 8 날짜 타입 지원 추가
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		// 3. 전역 코덱 설정
		// redissonClient를 사용하는 모든 곳에서 기본으로 사용됨
		config.setCodec(new JsonJacksonCodec(objectMapper));

		return Redisson.create(config);
	}
}
