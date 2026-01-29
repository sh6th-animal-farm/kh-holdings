package com.kanghwang.khholdings.domain.market;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;
import com.kanghwang.khholdings.domain.market.dto.OrderPriceDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;
import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.TradeDTO;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;
import com.kanghwang.khholdings.global.util.ApiResponseUtil;

@RestController
@RequestMapping("/api/market")
public class MarketController {

	@Autowired
	private MarketService marketService;

	@GetMapping()
	public ResponseEntity<ApiResponse<List<TokenListDTO>>> selectAll() {
		List<TokenListDTO> list = marketService.selectAll();
		if (list == null) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return ApiResponseUtil.ok("토큰 종목 조회에 성공했습니다.", list);
	}

	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<TokenListDTO>>> selectBySearch(@RequestParam(required = false) String content) {
		List<TokenListDTO> list = marketService.selectBySearch(content);
		if (list == null) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return ApiResponseUtil.ok("토큰 종목 검색에 성공했습니다.", list);
	}

	@GetMapping("/current/{tokenId}")
	public ResponseEntity<ApiResponse<BigDecimal>> getCurrentPrice(@PathVariable("tokenId") Long tokenId) {
		BigDecimal curPrice = marketService.getCurrentPrice(tokenId);
		if (curPrice == null) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.", null);
		}

		return ApiResponseUtil.ok("토큰 현재가 조회에 성공했습니다.", curPrice);
	}

	@GetMapping("/order/buy/{tokenId}")
	public ResponseEntity<ApiResponse<List<OrderPriceDTO>>> selectAllOrderBuyPrice(@PathVariable Long tokenId) {
		List<OrderPriceDTO> list = marketService.selectAllOrderBuyPrice(tokenId);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("매수 호가 조회에 성공했습니다.", list);
	}

	@GetMapping("/order/sell/{tokenId}")
	public ResponseEntity<ApiResponse<List<OrderPriceDTO>>> selectAllOrderSellPrice(@PathVariable Long tokenId) {
		List<OrderPriceDTO> list = marketService.selectAllOrderSellPrice(tokenId);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("매도 호가 조회에 성공했습니다.", list);
	}

	@GetMapping("/trade/{tokenId}")
	public ResponseEntity<ApiResponse<List<TradeDTO>>> selectAllTradePrice(@PathVariable Long tokenId) {
		List<TradeDTO> list = marketService.selectAllTradePrice(tokenId);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("체결 조회에 성공했습니다.", list);
	}

	@GetMapping("/candles/{tokenId}")
	public ResponseEntity<ApiResponse<List<CandleDTO>>> selectCandles(
            @PathVariable Long tokenId,
			@RequestParam(defaultValue = "1") int unit,
			@RequestParam(defaultValue = "200") int limit) {

		List<CandleDTO> list = marketService.selectCandles(tokenId, unit, limit);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("차트 조회에 성공했습니다.", list);
  }
    
	@GetMapping("/{tokenId}/pending/{walletId}")
	public ResponseEntity<ApiResponse<List<PendingDTO>>> selectPending(@PathVariable Long tokenId, @PathVariable Long walletId) {
		List<PendingDTO> list = marketService.selectPending(tokenId, walletId);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("미체결 내역 조회에 성공했습니다.", list);
	}
}
