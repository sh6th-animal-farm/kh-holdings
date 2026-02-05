package com.kanghwang.khholdings.domain.market.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.redisson.api.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.order.OrderRepository;
import com.kanghwang.khholdings.domain.market.dto.CandleDTO;
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
			local price = tonumber(ARGV[1])  -- targetPrice
			local vol = tonumber(ARGV[2])    -- executedVolume

			if not c[1] then
				-- 새로운 봉 생성
			    redis.call('HMSET', KEYS[1], 'open', price, 'high', price, 'low', price, 'close', price, 'vol', vol)
			else
			    -- 기존 봉 업데이트
			    if price > tonumber(c[1]) then redis.call('HSET', KEYS[1], 'high', price) end
			    if price < tonumber(c[2]) then redis.call('HSET', KEYS[1], 'low', price) end
			    redis.call('HSET', KEYS[1], 'close', price)
			    redis.call('HINCRBYFLOAT', KEYS[1], 'vol', vol)
			end
			redis.call('EXPIRE', KEYS[1], 10800) -- 현재 봉 데이터를 10800초(3시간) 동안만 유지
			""";

	// 1. OrderRedisService에서 호출됨
	public void processMarketUpdate(TransactionRequestDTO trade) {
		updateMarketSnapshot(trade);
		updateRedisCandle(trade);
	}

	// 2. 토큰 시세 리스트 실시간 업데이트 (현재가, 등락률, 거래대금)
	public void updateMarketSnapshot(TransactionRequestDTO trade) {
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getMarketInfoKey());
		TokenListDTO token = marketInfoMap.get(trade.getTokenId());

		if (token != null) {
			BigDecimal currentPrice = trade.getTargetPrice();

			// 현재가
			token.setMarketPrice(currentPrice);

			// 시가
			if (token.getOpenPrice() == null || token.getOpenPrice().compareTo(BigDecimal.ZERO) == 0) {
				token.setOpenPrice(currentPrice);
			}

			// 고가
			if (token.getHighPrice() == null || currentPrice.compareTo(token.getHighPrice()) > 0) {
				token.setHighPrice(currentPrice);
			}

			// 저가
			if (token.getLowPrice() == null || currentPrice.compareTo(token.getLowPrice()) < 0) {
				token.setLowPrice(currentPrice);
			}

			// 등락률
			if (token.getOpenPrice() != null && token.getOpenPrice().compareTo(BigDecimal.ZERO) > 0) {
				// 등락률 계산 = ((현재가 - 오늘 오전 9시 기준가) / 기준가) * 100
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
			// 추후 토큰 거래소 목록 조회 시, 정렬된 목록 반환
			redissonClient.getScoredSortedSet(redisKeyManager.getMarketRankKey())
					.addScore(trade.getTokenId(), amount.doubleValue());

			// 토큰 리스트에 정보 업데이트
			marketInfoMap.put(trade.getTokenId(), token);

			// 토큰 정보를 웹소켓을 통해 실시간으로 전파하여 토큰 실시간 리스트를 업데이트
			redissonClient.getTopic(redisKeyManager.getMarketUpdateTopicKey()).publish(token);
		}
	}

	// 1분 봉 실시간 생성 및 Websocket 전송
	public void updateRedisCandle(TransactionRequestDTO trade) {

		// 1분 단위로 버킷팅 (ex: 12:05:33 -> 12:05:00)
		long minute = trade.getCreatedAt().truncatedTo(ChronoUnit.MINUTES).toEpochSecond(); // 분까지 짜른 후, 타임스탬프로 변환
		String candleKey = redisKeyManager.getCandleKey(trade.getTokenId(), 1, minute);

		redissonClient.getScript(org.redisson.client.codec.StringCodec.INSTANCE).eval(
				RScript.Mode.READ_WRITE,
				luaScript, // 상단에서 작성했던 거
				RScript.ReturnType.VALUE,
				List.of(candleKey), // KEYS[1]
				trade.getTargetPrice().toPlainString(), // ARGV[1]
				trade.getExecutedVolume().toPlainString() // ARGV[2]
		);

		// 루아 스크립트를 통해 업데이트된 최신 캔들 정보를 읽어서 전파
		// 추후에 트레이딩뷰 차트를 실시간으로 움직이게 함
		Map<String, String> candleMap = redissonClient
				.<String, String>getMap(candleKey, org.redisson.client.codec.StringCodec.INSTANCE)
				.readAllMap();

		if (candleMap != null && !candleMap.isEmpty()) {
			String topicKey = redisKeyManager.getCandleTopicKey(trade.getTokenId());

			CandleDTO liveCandle = CandleDTO.builder()
					.tokenId(trade.getTokenId())
					.unit(1)
					.candleTime(minute)
					.openingPrice(new BigDecimal(candleMap.get("open")))
					.highPrice(new BigDecimal(candleMap.get("high")))
					.lowPrice(new BigDecimal(candleMap.get("low")))
					.closingPrice(new BigDecimal(candleMap.get("close")))
					.tradeVolume(new BigDecimal(candleMap.get("vol")))
					.build();

			// [MarketWoker - 차트]
			// DTO 자체를 Redis Topic으로 발행 (MarketWorker가 받음)
			redissonClient.getTopic(topicKey).publish(liveCandle);
		}
	}

	// 매일 오전 9시
	// 현재가, 등락률, 거래대금 갱신
	@Scheduled(cron = "0 0 9 * * *")
	public void resetDailyData() {
		redissonClient.getScoredSortedSet(redisKeyManager.getMarketRankKey()).clear();

		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getMarketInfoKey());
		RBatch batch = redissonClient.createBatch();

		for (Long tokenId : marketInfoMap.keySet()) {
			TokenListDTO dto = marketInfoMap.get(tokenId);
			if (dto == null) {
				continue;
			}

			BigDecimal openingPrice = dto.getMarketPrice();
			dto.setOpenPrice(openingPrice);
			dto.setHighPrice(openingPrice);
			dto.setLowPrice(openingPrice);
			dto.setDailyTradeVolume(BigDecimal.ZERO);
			dto.setChangeRate(BigDecimal.ZERO);

			// 배치에 모아뒀다가
			batch.getMap(redisKeyManager.getMarketInfoKey()).putAsync(tokenId, dto);
			batch.getTopic(redisKeyManager.getMarketUpdateTopicKey()).publishAsync(dto);
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

		// 2. 전체 토큰 정보 로드 (현재가 참조용)
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getMarketInfoKey());
		List<Long> tokenIds = new ArrayList<>(marketInfoMap.keySet());

		if (tokenIds.isEmpty()) return;

		// 3. Redisson Batch를 사용하여 Redis 조회 최적화
		RBatch batch = redissonClient.createBatch();
		for (Long tokenId : tokenIds) {
			// Redis에 해당 분의 실시간 캔들이 있는지 예약조회
			String candleKey = redisKeyManager.getCandleKey(tokenId, 1, lastMinute);
			batch.getMap(candleKey, org.redisson.client.codec.StringCodec.INSTANCE).readAllMapAsync();
		}

		List<Map<String, String>> redisResults = (List<Map<String, String>>) batch.execute().getResponses();
		List<CandleDTO> finalCandles = new ArrayList<>();

		int index = 0;
		for (Long tokenId : tokenIds) {
			Map<String, String> raw = redisResults.get(index++);
			TokenListDTO tokenInfo = marketInfoMap.get(tokenId);

			CandleDTO candle;
			if (raw != null && !raw.isEmpty()) {
				// [CASE 1] 거래가 있었던 경우: Redis 데이터 사용
				candle = CandleDTO.builder()
						.tokenId(tokenId).unit(1).candleTime(lastMinute)
						.openingPrice(new BigDecimal(raw.get("open")))
						.highPrice(new BigDecimal(raw.get("high")))
						.lowPrice(new BigDecimal(raw.get("low")))
						.closingPrice(new BigDecimal(raw.get("close")))
						.tradeVolume(new BigDecimal(raw.get("vol")))
						.build();
			} else {
				// [CASE 2] 거래가 없었던 경우: 이전 종가(현재가)로 더미 캔들 생성
				BigDecimal lastPrice = (tokenInfo != null) ? tokenInfo.getMarketPrice() : BigDecimal.ZERO;
				candle = CandleDTO.builder()
						.tokenId(tokenId).unit(1).candleTime(lastMinute)
						.openingPrice(lastPrice).highPrice(lastPrice)
						.lowPrice(lastPrice).closingPrice(lastPrice)
						.tradeVolume(BigDecimal.ZERO)
						.build();
			}
			finalCandles.add(candle);
		}

		// 4. DB 및 Redis Persistent Cache에 일괄 저장
		if (!finalCandles.isEmpty()) {

			// DB에 저장
			orderRepository.insertCandlesBatch(finalCandles);

			// Redis에 저장
			RBatch zsetBatch = redissonClient.createBatch();
			for (CandleDTO candle : finalCandles) {
				String topicKey = redisKeyManager.getCandleTopicKey(candle.getTokenId());
				String zsetKey = "candle:1m:" + candle.getTokenId();

				RScoredSortedSet<CandleDTO> zset = redissonClient.getScoredSortedSet(zsetKey);
				zset.addAsync((double) candle.getCandleTime(), candle);
				zset.removeRangeByRank(0, -1001);

				zsetBatch.getTopic(topicKey).publishAsync(candle);
			}

			zsetBatch.execute();

			log.info("[TradeWorker - Sync] {} 시점의 1분 봉 {}건을 DB로 저장 완료", lastMinute, finalCandles.size());
		}
	}
}
