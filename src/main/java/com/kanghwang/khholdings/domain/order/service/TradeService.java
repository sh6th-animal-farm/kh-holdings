package com.kanghwang.khholdings.domain.order.service;

import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

Service
@RequiredArgsConstructor
public class TradeService {
	private final TradeMapper tradeMapper; // MyBatis Mapper
	private final RedisTemplate<String, Object> redisTemplate;

	@Transactional
	public void completeTrade(TradeDto trade) {
		// 1. 핵심 자산 업데이트 (MyBatis 프로시저 호출)
		// 이 안에는 wallets, holdings, orders 업데이트 로직이 들어있음
		tradeMapper.p_update_asset_sync(trade);

		// 2. 비동기 파이프라인 가동
		// (1) 시세 전파용 Pub/Sub
		redisTemplate.convertAndSend("ticker:" + trade.getTokenId(), trade);

		// (2) 이력 저장용 Streams
		redisTemplate.opsForStream().add(
			StreamRecords.newRecord().in("trade:stream").ofObject(trade)
		);
	}
}
