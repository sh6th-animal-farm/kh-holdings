package com.kanghwang.khholdings.domain.market.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.market.dto.OrderPriceDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;
import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.TradeDTO;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

	private final MarketRepository marketRepository;
	private final RedissonClient redissonClient;
	private final RedisKeyManager redisKeyManager;

	// 종목 전체 조회
	public List<TokenListDTO> selectAll() {

		long start = System.currentTimeMillis();
		log.info("API 시작");

		// 1. Redis Map에서 실시간 데이터 조회
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");
		List<TokenListDTO> list = new ArrayList<>(marketInfoMap.readAllValues());

		if (list.isEmpty()) {
			list = marketRepository.selectAll();

			if (list != null && !list.isEmpty()) {

				RScoredSortedSet<Long> rankingSet = redissonClient.getScoredSortedSet(redisKeyManager.getPrefix() + "market:ranking");
				list.forEach(dto -> {
					if (dto.getDailyTradeVolume() != null) {
						dto.setDailyTradeVolume(dto.getDailyTradeVolume().setScale(0, RoundingMode.DOWN));
					}
					if (dto.getChangeRate() != null) {
						dto.setChangeRate(dto.getChangeRate().setScale(2, RoundingMode.HALF_UP));
					}
					rankingSet.add(dto.getDailyTradeVolume().doubleValue(), dto.getTokenId());
				});

				Map<Long, TokenListDTO> map = list.stream()
						.collect(Collectors.toMap(TokenListDTO::getTokenId, dto -> dto));
				marketInfoMap.putAll(map);
			}
		}

		list.sort((a, b) -> {
			BigDecimal volA = a.getDailyTradeVolume() != null ? a.getDailyTradeVolume() : BigDecimal.ZERO;
			BigDecimal volB = b.getDailyTradeVolume() != null ? b.getDailyTradeVolume() : BigDecimal.ZERO;
			return volB.compareTo(volA);
		});

		log.info("로직 완료까지 걸린 시간: {}ms", (System.currentTimeMillis() - start));

		return list == null ? new ArrayList<>() : list;
	}

	// 종목 검색어 조회
	public List<TokenListDTO> selectBySearch(String content) {
		return marketRepository.selectBySearch(content);
	}

	// 차트 조회
	public List<String> selectCandles(Long tokenId, String unit, long start, long end) {

		String redisKey = String.format("candles:%s:%d", unit, tokenId);
		RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet(redisKey);

		// 1. redis 범위 조회 (Score : Timestamp)
		Collection<String> cached = zset.valueRange(start, true, end, true);
		if(!cached.isEmpty()) {
			return new ArrayList<>(cached);
		}

		// 2. redis에 없을 시 DB 로드 및 redis에 저장
		RLock lock = redissonClient.getLock("lock:" + redisKey);
		try {
			if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
				// 재확인
				cached = zset.valueRange(start, true, end, true);
				if (!cached.isEmpty()) {
					return new ArrayList<>(cached);
				}

				// DB 조회 및 CSV 변환
				List<CandleDTO> dbData = marketRepository.selectCandles(tokenId, unit, start, end);
				if (dbData.isEmpty()) {
					return new ArrayList<>();
				}

				Map<String, Double> toCache = new HashMap<>();
				for (CandleDTO candle : dbData) {
					toCache.put(candle.toCsv(), (double)candle.getCandleTime());
				}

				zset.addAll(toCache);
				return dbData.stream()
						.map(CandleDTO::toCsv)
						.toList();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();;
			}
		}
		return new ArrayList<>();
	}

	// 미체결 내역 조회
	public List<PendingDTO> selectPending(Long tokenId, Long walletId) {
		return marketRepository.selectPending(tokenId, walletId);
	}

	// 현재가 조회
	public BigDecimal getCurrentPrice(Long tokenId) { return marketRepository.selectLatestTokenPrice(tokenId); }

	// 매수 호가 조회
	public List<OrderPriceDTO> selectAllOrderBuyPrice(Long tokenId) {
		BigDecimal price = getCurrentPrice(tokenId);
		return marketRepository.selectAllOrderBuyPrice(tokenId, price);
	}

	// 매도 호가 조회
	public List<OrderPriceDTO> selectAllOrderSellPrice(Long tokenId) {
		BigDecimal price = getCurrentPrice(tokenId);
		return marketRepository.selectAllOrderSellPrice(tokenId, price);
	}

	// 체결 조회
	public List<TradeDTO> selectAllTradePrice(Long tokenId) {
		return marketRepository.selectAllTradePrice(tokenId);
	}
}
