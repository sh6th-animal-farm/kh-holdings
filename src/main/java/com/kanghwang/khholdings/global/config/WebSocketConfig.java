package com.kanghwang.khholdings.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // @EnableWebSocketMessageBroker 어노테이션은 한마디로 **"우리 스프링 서버에서 STOMP를 사용한 고성능 메시지 브로커 기능을 활성화하겠다!
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 프론트가 연결할 엔드포인트: ws://localhost:8080/ws-stomp
		registry.addEndpoint("/ws-stomp")
				.setAllowedOriginPatterns("*") // 테스트용 모든 도메인 허용
				.withSockJS() // 구형 브라우저 지원용
				.setHeartbeatTime(10000); // 10초마다 생존 확인
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// 서버가 사용자에게 메시지를 보낼 때 (구독 경로), topic(공용), queue(개인용)
		registry.enableSimpleBroker("/topic", "/queue");
		// 사용자가 서버로 메시지를 보낼 때 (보내는 경로)
		registry.setApplicationDestinationPrefixes("/app");

		registry.setPreservePublishOrder(true);
	}
}
