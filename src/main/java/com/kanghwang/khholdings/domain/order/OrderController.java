package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.service.OrderService;
import com.kanghwang.khholdings.global.dto.ApiResponse;
import com.kanghwang.khholdings.global.util.ApiResponseUtil;

@RestController
@RequestMapping("/api/order")
public class OrderController {

	@Autowired
	OrderService orderService;

	// 해당 토큰 보유 수량 조회
	@GetMapping("/balance/{walletId}/{tokenId}")
	public ResponseEntity<ApiResponse<BigDecimal>> selectHoldingTokenBalance(@PathVariable Long walletId, @PathVariable Long tokenId) {
		BigDecimal data = orderService.selectHoldingTokenBalance(walletId, tokenId);
		if (data == null) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.", null);
		}

		return ApiResponseUtil.ok("토큰 보유 수량 조회에 성공했습니다.", data);
	}

	// 주문 가능 금액 조회
	@GetMapping("/balance/{walletId}")
	public ResponseEntity<ApiResponse<BigDecimal>> selectAvailableBalance(@PathVariable Long walletId) {
		BigDecimal data = orderService.selectAvailableBalance(walletId);
		if (data == null) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.", null);
		}

		return ApiResponseUtil.ok("주문 가능 금액 조회에 성공했습니다.", data);
	}

	// 매수/매도 주문
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> placeOrder(@RequestBody OrderRequestDTO orderDTO) {
		orderService.placeOrder(orderDTO);
		return ApiResponseUtil.ok("주문이 완료되었습니다.", null);
	}

	// 주문 취소
	@PostMapping("/cancel/{tokenId}/{orderId}")
	public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long tokenId, @PathVariable Long orderId) {
		orderService.cancelOrder(tokenId, orderId);
		return ApiResponseUtil.ok("주문 취소가 완료되었습니다.", null);
	}
}
