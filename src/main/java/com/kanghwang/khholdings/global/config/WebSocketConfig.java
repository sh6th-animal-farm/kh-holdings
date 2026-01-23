package com.kanghwang.khholdings.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 프론트가 연결할 엔드포인트: ws://localhost:8080/ws-stomp
		registry.addEndpoint("/ws-stomp")
			.setAllowedOriginPatterns("*") // 테스트용 모든 도메인 허용
			.withSockJS(); // 구형 브라우저 지원용
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// 서버가 사용자에게 메시지를 보낼 때 (구독 경로)
		registry.enableSimpleBroker("/topic");
		// 사용자가 서버로 메시지를 보낼 때 (보내는 경로)
		registry.setApplicationDestinationPrefixes("/app");
	}
}
