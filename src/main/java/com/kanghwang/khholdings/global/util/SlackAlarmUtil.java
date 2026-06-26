package com.kanghwang.khholdings.global.util;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackAlarmUtil {
	private final RestTemplate restTemplate;

	@Value("${slack.webhook.url}")
	private String slackUrl;

	public void sendAlarm(String message) {
		try {
			Map<String, String> body = Map.of("text", message);
			restTemplate.postForEntity(slackUrl, body, String.class);
		} catch (Exception e) {
			log.error("슬랙 알림 전송 중 에러 발생", e);
		}
	}
}
