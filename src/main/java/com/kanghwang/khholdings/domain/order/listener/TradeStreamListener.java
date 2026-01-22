package com.kanghwang.khholdings.domain.order.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeStreamListener {

	private final RedisTemplate<String, Object> redisTemplate;

	public void publishTradeEvent(TradeDto trade) {
		// 1. 시세 전파 (Pub/Sub): 속도가 생명!
		// 채널명: ticker:{tokenId} (예: ticker:7777)
		redisTemplate.convertAndSend("ticker:" + trade.getTokenId(), trade);

		// 2. 이력 저장용 (Streams): 유실 방지가 생명!
		// 'mystream'이라는 스트림에 데이터를 추가
		ObjectRecord<String, TradeDto> record = StreamRecords.newRecord()
			.in("trade:stream")
			.lastIds()
			.ofObject(trade);

		redisTemplate.opsForStream().add(record);
	}
}
