package com.kanghwang.khholdings.domain.market.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.redisson.api.RBatch;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSetAsync;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.dto.CandleDTO;
import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.order.OrderRepository;
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

	/*
	Lua Script를 이용한 실시간 봉 업데이트
	- 데이터가 없으면 ohlc를 모두 현재가로 하는 새로운 봉 생성
	- 데이터가 있으면 기존 봉 업데이트
	- 24시간(86400초) 뒤 Redis에서 자동 삭제
	 */
	private static final String luaScript = """
		local c = redis.call('HMGET', KEYS[1], 'high', 'low', 'close', 'vol') -- candleKey
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
		redis.call('EXPIRE', KEYS[1], 86400) -- 현재 봉 데이터를 24시간 동안 유지
	""";

	// OrderRedisService에서 체결 발생 시마다 호출
	public void processMarketUpdate(TransactionRequestDTO trade) {
		updateRedisMarket(trade); // 실시간 시세 업데이트
		updateRedisCandle(trade); // 실시간 봉 업데이트
	}

	/*
	실시간 시세 업데이트 및 전파
	- 토큰 목록 정보(OHLCV, 등락률)를 실시간으로 갱신
	- 업데이트된 토큰 정보를 Redis Topic으로 발행하여 웹소켓을 통해 클라이언트에게 즉시 전달
	 */
	public void updateRedisMarket(TransactionRequestDTO trade) {
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getMarketInfoKey());
		TokenListDTO token = marketInfoMap.get(trade.getTokenId());

		if (token != null) {
			BigDecimal currentPrice = trade.getTargetPrice();

			// OHLCV 및 등락률 업데이트
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
						.stripTrailingZeros() // 1.2500 -> 1.25 로 불필요한 0 삭제
						.setScale(2, RoundingMode.HALF_UP); // 반올림하여 소수점 이하 2자리 수까지만 유지
				token.setChangeRate(rate);
			}

			// 거래대금
			BigDecimal amount = trade.getTargetPrice().multiply(trade.getExecutedVolume());
			BigDecimal newTotalVolume = token.getDailyTradeVolume().add(amount)
					.setScale(0, RoundingMode.DOWN);
			token.setDailyTradeVolume(newTotalVolume);

			// Redis 업데이트
			// (1) 거래대금에 따른 순위 (높은순, 내림차순)
			// 추후 토큰 거래소 목록 조회 시, 정렬된 목록 반환
			redissonClient.getScoredSortedSet(redisKeyManager.getMarketRankKey())
					.addScore(trade.getTokenId(), amount.doubleValue());

			// (2) 토큰 리스트에 정보 업데이트
			marketInfoMap.put(trade.getTokenId(), token);

			// (3) 토큰 정보를 웹소켓을 통해 실시간으로 전파하여 토큰 실시간 리스트를 업데이트
			redissonClient.getTopic(redisKeyManager.getMarketUpdateTopicKey()).publish(token);
		}
	}

	// 봉 실시간 생성
	public void updateRedisCandle(TransactionRequestDTO trade) {
		// 1분 단위 타임스탬프 (ex: 12:08:33 -> 12:08:00)
		long min1 = trade.getCreatedAt().truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
		processCandleUpdate(trade, 1, min1);

		// 5분 단위 타임스탬프 (ex: 12:08:33 -> 12:05:33)
		long min5 = (min1 / 300) * 300;
		processCandleUpdate(trade, 5, min5);

		// 15분 단위 (0, 15, 30, 45)
		long min15 = (min1 / 900) * 900;
		processCandleUpdate(trade, 15, min15);

		// 60분 단위 (매시 정각)
		long min60 = (min1 / 3600) * 3600;
		processCandleUpdate(trade, 60, min60);
	}

	// 실시간 봉 생성(업데이트) 및 Websocket 전송
	private void processCandleUpdate(TransactionRequestDTO trade, int unit, long timestamp) {
		Long tokenId = trade.getTokenId();
		String candleKey = redisKeyManager.getCandleKey(tokenId, unit, timestamp);

		// 1. 캔들 정보 업데이트
		// 위에서 작성한 루아스크립트를 활용해 현재 단위 시간에 대한 봉 생성 또는 업데이트
		redissonClient.getScript(org.redisson.client.codec.StringCodec.INSTANCE).eval(
				RScript.Mode.READ_WRITE,
				luaScript,
				RScript.ReturnType.VALUE,
				List.of(candleKey), // KEYS[1]
				trade.getTargetPrice().toPlainString(), // ARGV[1]
				trade.getExecutedVolume().toPlainString() // ARGV[2]
		);

		// 2. 캔들 정보 전파
		// 루아스크립트를 통해 업데이트된 최신 캔들 정보를 읽어서 전파
		Map<String, String> candleMap = redissonClient
				.<String, String>getMap(candleKey, org.redisson.client.codec.StringCodec.INSTANCE)
				.readAllMap();

		if (candleMap != null && !candleMap.isEmpty()) {
			String topicKey = redisKeyManager.getCandleTopicKey(tokenId, unit);
			CandleDTO liveCandle = buildCandleDTO(tokenId, unit, timestamp, candleMap);

			// candleDTO를 Redis Topic으로 발행 (MarketWorker가 받음)
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
	// 봉 데이터 저장하기 위한 함수 호출
	@Scheduled(cron = "5 * * * * *")
	public void syncCandleToDB() {// 바로 직전 분의 타임스탬프 (마감된 봉)
		long lastMinute = OffsetDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
		long currentPoint = lastMinute + 60; // 현재 시점(초)

		// 1. 매 분 1분 봉 저장
		saveCandlesToDbAndRedis(lastMinute, 1);

		// 2. 5분 봉 저장 (5, 10, 15...분 정각일 때 직전 5분치 저장)
		if (currentPoint % 300 == 0) {
			long last5Minute = (lastMinute / 300) * 300;
			saveCandlesToDbAndRedis(last5Minute, 5);
		}

		// 3. 15분 봉 저장 (15, 30, 45, 0분 정각일 때)
		if (currentPoint % 900 == 0) {
			long last15Minute = (lastMinute / 900) * 900;
			saveCandlesToDbAndRedis(last15Minute, 15);
		}

		// 4. 60분 봉 저장 (매시 정각일 때)
		if (currentPoint % 3600 == 0) {
			long last60Minute = (lastMinute / 3600) * 3600;
			saveCandlesToDbAndRedis(last60Minute, 60);
		}
	}

	/*
	봉 데이터 일괄 저장
	- Redis Batch를 이용하여 한 번에 조회
	- Redis에 봉 데이터가 있으면 실제 봉 생성
	- 데이터가 없으면 이전 종가와 거래량이 0인 더미 봉 생성
	 */
	private void saveCandlesToDbAndRedis(long timestamp, int unit) {
		// 1. 전체 토큰 정보 로드 (현재가 참조용)
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getMarketInfoKey());
		List<Long> tokenIds = new ArrayList<>(marketInfoMap.keySet());

		if (tokenIds.isEmpty()) return;

		// 2. Redisson Batch를 사용하여 Redis 조회 최적화
		RBatch liveBatch = redissonClient.createBatch();
		for (Long tokenId : tokenIds) {
			// Redis에 해당 분의 실시간 캔들 정보가 있는지 예약 조회
			String candleKey = redisKeyManager.getCandleKey(tokenId, unit, timestamp);
			liveBatch.getMap(candleKey, org.redisson.client.codec.StringCodec.INSTANCE).readAllMapAsync();
		}

		// 모든 토큰에 대해 실시간 캔들 정보 조회
		List<Map<String, String>> redisResults = (List<Map<String, String>>)liveBatch.execute().getResponses();

		// 3. candle 생성 및 거래 없을 때 더미 캔들 실시간 전파
		List<CandleDTO> finalCandles = new ArrayList<>();
		RBatch dummyBatch = redissonClient.createBatch();
		int index = 0;

		for (Long tokenId : tokenIds) {
			Map<String, String> raw = redisResults.get(index++);
			TokenListDTO tokenInfo = marketInfoMap.get(tokenId);
			String topicKey = redisKeyManager.getCandleTopicKey(tokenId, unit);

			if (raw != null && !raw.isEmpty()) {
				// [CASE 1] 거래가 있었던 경우: Redis 데이터 사용
				finalCandles.add(buildCandleDTO(tokenId, unit, timestamp, raw));
			} else {
				// [CASE 2] 거래가 없었던 경우: 이전 종가(현재가)로 더미 캔들 생성
				BigDecimal lastPrice = (tokenInfo != null) ? tokenInfo.getMarketPrice() : BigDecimal.ZERO;
				CandleDTO dummyCandle = buildDummyCandle(tokenId, unit, timestamp, lastPrice);

				finalCandles.add(dummyCandle); // DB
				dummyBatch.getTopic(topicKey).publishAsync(dummyCandle); // Redis
			}
		}

		// 4. DB에 최종 봉 일괄 저장
		if (!finalCandles.isEmpty()) {

			// DB에 저장
			orderRepository.insertCandlesBatch(finalCandles);

			// Redis에 저장 및 더미 캔들 전파
			RBatch cacheBatch = redissonClient.createBatch();
			for (CandleDTO candle : finalCandles) {
				String cacheKey = "candle:" + unit + "m:" + candle.getTokenId();
				RScoredSortedSetAsync<CandleDTO> zset = cacheBatch.getScoredSortedSet(cacheKey);
				zset.addAsync((double)candle.getCandleTime(), candle);
				zset.removeRangeByRankAsync(0, -1001); // 최신 1000개 데이터만 유지
			}

			cacheBatch.execute(); // 캐시 업데이트
			dummyBatch.execute(); // 더미 캔들 전송
			log.info("[TradeWorker - Sync] {}분 봉 {}건 저장 완료 (Time: {})", unit, finalCandles.size(), timestamp);
		}
	}

	// 실제 캔들 빌더
	private CandleDTO buildCandleDTO(Long id, int unit, long time, Map<String, String> raw) {
		return CandleDTO.builder().tokenId(id).unit(unit).candleTime(time)
			.openingPrice(new BigDecimal(raw.get("open"))).highPrice(new BigDecimal(raw.get("high")))
			.lowPrice(new BigDecimal(raw.get("low"))).closingPrice(new BigDecimal(raw.get("close")))
			.tradeVolume(new BigDecimal(raw.get("vol"))).build();
	}

	// 가짜 캔들 빌더
	private CandleDTO buildDummyCandle(Long id, int unit, long time, BigDecimal price) {
		return CandleDTO.builder().tokenId(id).unit(unit).candleTime(time)
			.openingPrice(price).highPrice(price).lowPrice(price).closingPrice(price)
			.tradeVolume(BigDecimal.ZERO).build();
	}
}
