package com.kanghwang.khholdings.domain.market.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;
import com.kanghwang.khholdings.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
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
		// Redis에서 실시간으로 덮어쓰기 되고 있는 현재 데이터 맵 가져오기
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");

		List<TokenListDTO> list = new ArrayList<>(marketInfoMap.values());
		if (list.isEmpty()) {
			System.out.println("DBDBDBDBDBDBDBDB");
			list = marketRepository.selectAll();

			Map<Long, TokenListDTO> tempMap = list.stream()
					.collect(Collectors.toMap(TokenListDTO::getTokenId, dto -> dto));
			marketInfoMap.putAll(tempMap);
		}
		// 거래대금으로 내림차순 정렬
		list.sort((a, b) -> {
			BigDecimal volA = a.getDailyTradeVolume() != null ? a.getDailyTradeVolume() : BigDecimal.ZERO;
			BigDecimal volB = b.getDailyTradeVolume() != null ? b.getDailyTradeVolume() : BigDecimal.ZERO;
			return volB.compareTo(volA);
		});

		return list;
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
	public List<PendingDTO>	selectPending(Long tokenId, Long walletId) {
		return marketRepository.selectPending(tokenId, walletId);
	}
}
