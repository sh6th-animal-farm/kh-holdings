package com.kanghwang.khholdings.domain.market.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;
import com.kanghwang.khholdings.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;

@Service
@RequiredArgsConstructor
public class MarketService {

	private final MarketRepository marketRepository;
	private final RedissonClient redissonClient;
	private final RedisKeyManager redisKeyManager;

	// 종목 전체 조회
	public List<TokenListDTO> selectAll() {

		// 1. Redis Map에서 실시간 데이터 조회
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getPrefix() +"market:info");
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

		return list == null ? new ArrayList<>() : list;
	}

	// 종목 검색어 조회
	public List<TokenListDTO> selectBySearch(String content) {
		return marketRepository.selectBySearch(content);
	}

	// 차트 조회
	public List<CandleDTO> selectCandles(Long tokenId, int unit, int limit) {
		return marketRepository.selectCandles(tokenId, unit, limit);
	}

	// 미체결 내역 조회
	public List<PendingDTO> selectPending(Long tokenId, Long walletId) {
		return marketRepository.selectPending(tokenId, walletId);
	}
}
