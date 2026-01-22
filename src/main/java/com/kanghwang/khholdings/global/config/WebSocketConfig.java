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
		// 클라이언트가 웹소켓에 접속할 경로 (예: ws://localhost:8080/ws-kh)
		registry.addEndpoint("/ws-kh")
			.setAllowedOriginPatterns("*") // 모든 도메인 허용 (테스트용)
			.withSockJS(); // 구형 브라우저 지원용
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// 서버가 클라이언트에게 메시지를 보낼 때 붙이는 접두사
		// 예: /topic/ticker/7777
		registry.enableSimpleBroker("/topic");

		// 클라이언트가 서버로 메시지를 보낼 때 붙이는 접두사
		registry.setApplicationDestinationPrefixes("/app");
	}
}
