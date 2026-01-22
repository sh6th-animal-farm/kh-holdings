package com.kanghwang.khholdings.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		// Key는 일반 문자열로 저장
		template.setKeySerializer(new StringRedisSerializer());

		// Value는 JSON 객체로 자동 변환하여 저장 (중요!)
		// 이렇게 해야 TradeDto 같은 객체를 바로 던질 수 있습니다.
		template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

		return template;
	}
}
