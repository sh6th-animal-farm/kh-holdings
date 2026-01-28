package com.kanghwang.khholdings.domain.market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.kanghwang.khholdings.domain.market.Service.MarketService;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import com.kanghwang.khholdings.global.util.RedisKeyManager;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

	private final MarketService marketService;
	private final RedissonClient redissonClient;
	private final RedisKeyManager redisKeyManager;

	// 전체 토큰 목록(시세) 조회
	@GetMapping()
	public ResponseEntity<ApiResponse<List<TokenListDTO>>> selectAll() {
		// Redis에서 실시간으로 덮어쓰기 되고 있는 현재 데이터 맵 가져오기
		RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getPrefix() + "market:info");

		List<TokenListDTO> list = new ArrayList<>(marketInfoMap.values());

		// 거래대금으로 내림차순 정렬
		list.sort((a, b) -> b.getDailyTradeVolume().compareTo(a.getDailyTradeVolume()));

		if (list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("토큰 종목 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("토큰 종목 조회에 성공했습니다.", list));
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<TokenListDTO>>> selectBySearch(@RequestParam(required = false) String content) {
		List<TokenListDTO> list = marketService.selectBySearch(content);

		if (list == null || list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("토큰 종목 검색에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("토큰 종목 검색에 성공했습니다.", list));
	}

	@GetMapping("/candles/{tokenId}")
	public ResponseEntity<ApiResponse<List<CandleDTO>>> selectCandles(
            @PathVariable Long tokenId,
			@RequestParam(defaultValue = "1") int unit,
			@RequestParam(defaultValue = "200") int limit) {

		List<CandleDTO> list = marketService.selectCandles(tokenId, unit, limit);

		if (list == null || list.isEmpty()) {
			// 거래가 없었던 데이터라면 잘못된 요청이 아니라서 빈 리스트 반환
			return ResponseEntity.ok(ApiResponse.success("데이터가 없습니다.", Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResponse.success("차트 조회에 성공했습니다.", list));
  }
    
	@GetMapping("/{tokenId}/pending")
	public ResponseEntity<ApiResponse<List<PendingDTO>>> selectPending(@PathVariable Long tokenId, @RequestParam Long walletId) {
		List<PendingDTO> list = marketService.selectPending(tokenId, walletId);

		if (list == null || list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("미체결 내역이 존재하지 않습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("미체결 내역 조회에 성공했습니다.", list));
	}
}
