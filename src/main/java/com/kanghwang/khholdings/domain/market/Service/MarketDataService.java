package com.kanghwang.khholdings.domain.market.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.kanghwang.khholdings.domain.market.type.UnitEnum;
import org.redisson.api.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.order.OrderRepository;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

	private final RedissonClient redissonClient;
	private final OrderRepository orderRepository;
	private final RedisKeyManager redisKeyManager;

	// 1분 봉 업데이트 용 Lua Script
	// 3시간 뒤 자동 삭제
	private static final String luaScript = """
            local c = redis.call('HMGET', KEYS[1], 'high', 'low', 'close', 'vol')
            local price = tonumber(ARGV[1])
            local vol = tonumber(ARGV[2])

            if not c[1] then
                redis.call('HMSET', KEYS[1], 'open', price, 'high', price, 'low', price, 'close', price, 'vol', vol)
            else
                -- 기존 봉 업데이트
                if price > tonumber(c[1]) then redis.call('HSET', KEYS[1], 'high', price) end
                if price < tonumber(c[2]) then redis.call('HSET', KEYS[1], 'low', price) end
                redis.call('HSET', KEYS[1], 'close', price)
                redis.call('HINCRBYFLOAT', KEYS[1], 'vol', vol)
            end
            redis.call('EXPIRE', KEYS[1], 10800)
            """;

	// 1. OrderRedisService에서 호출됨
	public void processMarketUpdate(TransactionRequestDTO trade) {
		updateMarketSnapshot(trade);
		updateRedisCandle(trade);
	}

	// 2. 토큰 시세 리스트 실시간 업데이트 (현재가, 등락률, 거래대금)
	public void updateMarketSnapshot(TransactionRequestDTO trade) {
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");
		TokenListDTO token = marketInfoMap.get(trade.getTokenId());

		if(token != null) {
			// 현재가
			token.setMarketPrice(trade.getTargetPrice());

			if (token.getOpenPrice() != null && token.getOpenPrice().compareTo(BigDecimal.ZERO) > 0) {
				// 등락률 계산 = ((현재가 - 오늘 오전 9시 기준가) /  기준가) * 100
				BigDecimal rate = trade.getTargetPrice().subtract(token.getOpenPrice())
					.divide(token.getOpenPrice(), 4, RoundingMode.HALF_UP)
					.multiply(new BigDecimal("100"))
					.stripTrailingZeros() // 1.2500 -> 1.25 로 깔끔하게 정리
					.setScale(2, RoundingMode.HALF_UP);
				token.setChangeRate(rate);
			}
			// 거래대금
			BigDecimal amount = trade.getTargetPrice().multiply(trade.getExecutedVolume());
			BigDecimal newTotalVolume = token.getDailyTradeVolume().add(amount)
					.setScale(0, RoundingMode.DOWN);
			token.setDailyTradeVolume(newTotalVolume);

			// 거래대금에 따른 순위
			redissonClient.getScoredSortedSet(redisKeyManager.getPrefix() +"market:ranking")
					.addScore(trade.getTokenId(), amount.doubleValue());

			marketInfoMap.put(trade.getTokenId(), token);

			redissonClient.getTopic(redisKeyManager.getPrefix() +"market:update:topic").publish(token);
		}
	}

	// 1분 봉 실시간 생성 및 Websocket 전송
	public void updateRedisCandle(TransactionRequestDTO trade) {

		// 1분 단위로 버킷팅 (ex: 12:05:33 -> 12:05:00
		long minute = trade.getCreatedAt().truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
		String candleKey = "candle:1m:" + trade.getTokenId() + ":" + minute;

		redissonClient.getScript(org.redisson.client.codec.StringCodec.INSTANCE).eval(
			RScript.Mode.READ_WRITE,
			luaScript, // 상단에서 작성했던 거
			RScript.ReturnType.VALUE,
			List.of(candleKey),
			trade.getTargetPrice().toPlainString(),
			trade.getExecutedVolume().toPlainString());

		// 업데이트된 최신 캔들 정보를 읽어서 전파
		// 추후에 트레이딩뷰 차트를 실시간으로 움직이게 함
		Map<String, String> candleMap = redissonClient
			.<String, String>getMap(candleKey, org.redisson.client.codec.StringCodec.INSTANCE)
			.readAllMap();

		if (candleMap != null && !candleMap.isEmpty()) {
			String topicKey = redisKeyManager.getCandleTopicKey(trade.getTokenId());

			CandleDTO liveCandle = CandleDTO.builder()
				.tokenId(trade.getTokenId())
				.unit(UnitEnum.MIN_1.getMinutes())
				.candleTime(minute)
				.openingPrice(new BigDecimal(candleMap.get("open")))
				.highPrice(new BigDecimal(candleMap.get("high")))
				.lowPrice(new BigDecimal(candleMap.get("low")))
				.closingPrice(new BigDecimal(candleMap.get("close")))
				.tradeVolume(new BigDecimal(candleMap.get("vol")))
				.build();

			// [MarketWoker - 차트]
			// DTO 자체를 Redis Topic으로 발행 (MarketWorker가 받음)
			redissonClient.getTopic(topicKey).publish(liveCandle.toCsv());
		}
	}

	// 매일 오전 9시
	// 현재가, 등락률, 거래대금 갱신
	@Scheduled(cron = "0 0 9 * * *")
	public void resetDailyData() {
		redissonClient.getScoredSortedSet(redisKeyManager.getPrefix() + "market:ranking").clear();

		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");
		RBatch batch = redissonClient.createBatch();

		for(Long tokenId : marketInfoMap.keySet()) {
			TokenListDTO dto = marketInfoMap.get(tokenId);
			if(dto == null) continue;

			dto.setOpenPrice(dto.getMarketPrice());
			dto.setDailyTradeVolume(BigDecimal.ZERO);
			dto.setChangeRate(BigDecimal.ZERO);

			// 배치에 모아뒀다가
			batch.getMap("market:info").putAsync(tokenId, dto);
			batch.getTopic("market:update:topic").publishAsync(dto);
			System.out.println("[MarketDataService] 9시 초기화 dto: " + dto.toString());
		}
		// 한 번에 redis로 전송
		batch.execute();
		log.info("[MarketScheduler] 오전 9시 마켓 데이터 초기화 완료");
	}

	// 매 분 5초
	// 1분 봉 생성
	@Scheduled(cron = "5 * * * * *")
	public void syncCandleToDB() {
		// 1. 방금 마감된 1분 계산
		long lastMinute = OffsetDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
		String pattern = "candle:1m:*:" + lastMinute;

		try {
			// 2. 해당 시간내의 모든 토큰 키 찾기
			Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(pattern);

			// 3. Redisson Batch 기능을 써서 한꺼번에 읽기 준비
			RBatch batch = redissonClient.createBatch();
			List<String> keyList = new ArrayList<>();

			for (String key : keys) {
				keyList.add(key);
				// "나중에 이 키들 데이터 한꺼번에 읽어올 거야"라고 예약만 함
				batch.getMap(key, org.redisson.client.codec.StringCodec.INSTANCE).readAllMapAsync();
			}

			if (keyList.isEmpty()) {
				return;
			}

			// 4. 한 번의 네트워크 통신으로 모든 데이터 수집
			BatchResult<?> result = batch.execute();
			List<Map<String, String>> allRawData = (List<Map<String, String>>) result.getResponses();

			// 5. 데이터 가공 (String -> BigDecimal & DTO화)
			List<CandleDTO> candleList = new ArrayList<>();
			for (int i = 0; i < keyList.size(); i++) {

				Map<String, String> raw = allRawData.get(i);
				if (raw == null || raw.isEmpty()) continue;

				// 토큰 아이디 추출
				String[] parts = keyList.get(i).split(":");
				Long tokenId = Long.valueOf(parts[2]);

				CandleDTO candle = CandleDTO.builder()
					.tokenId(tokenId)
					.unit(UnitEnum.MIN_1.getMinutes())
					.candleTime(lastMinute)
					.openingPrice(new BigDecimal(raw.get("open")))
					.highPrice(new BigDecimal(raw.get("high")))
					.lowPrice(new BigDecimal(raw.get("low")))
					.closingPrice(new BigDecimal(raw.get("close")))
					.tradeVolume(new BigDecimal(raw.get("vol")))
					.build();

				candleList.add(candle);
			}

			// 5. 1분 봉 저장
			if (!candleList.isEmpty()) {
				// DB에 저장
				orderRepository.insertCandlesBatch(candleList);

				// Redis에 저장
				for (CandleDTO candle : candleList) {
					String zsetKey = "candle:1m:" + candle.getTokenId();
					RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet(zsetKey);
					zset.add((double)candle.getCandleTime(), candle.toCsv());
					zset.removeRangeByRank(0, -1001);
					// zset.expire(24, TimeUnit.HOURS);
				}

				log.info("[TradeWorker - Sync] {} 시점의 1분 봉 {}건을 DB로 저장 완료", lastMinute, candleList.size());
			}

		} catch (Exception e) {
			log.error("[TradeWorker - Sync] 1분 봉 DB 동기화 중 오류 발생: ", e);
		}
	}
}
