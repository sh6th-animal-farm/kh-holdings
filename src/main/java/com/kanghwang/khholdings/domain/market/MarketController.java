package com.kanghwang.khholdings.domain.market;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
