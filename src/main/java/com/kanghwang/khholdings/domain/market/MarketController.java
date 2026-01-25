package com.kanghwang.khholdings.domain.market;

import java.util.Collections;
import java.util.List;

import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;

@RestController
@RequestMapping("/api/market")
public class MarketController {

	@Autowired
	private MarketService marketService;

	@GetMapping()
	public ResponseEntity<ApiResponse<List<MarketDTO>>> selectAll() {
		List<MarketDTO> list = marketService.selectAll();

		if (list == null || list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("토큰 종목 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("토큰 종목 조회에 성공했습니다.", list));
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<MarketDTO>>> selectBySearch(@RequestParam(required = false) String content) {
		List<MarketDTO> list = marketService.selectBySearch(content);

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
}
