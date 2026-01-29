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

@RestController
@RequestMapping("/api/order")
public class OrderController {

	@Autowired
	OrderService orderService;

	// 해당 토큰 보유 수량 조회
	@GetMapping("/balance/{walletId}/{tokenId}")
	public ResponseEntity<ApiResponse<BigDecimal>> selectHoldingTokenBalance(@PathVariable Long walletId, @PathVariable Long tokenId) {
		BigDecimal tokenBalance = orderService.selectHoldingTokenBalance(walletId, tokenId);
		if (tokenBalance == null) {
			return ResponseEntity.badRequest().body(ApiResponse.error("토큰 보유 수량 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("보유 토큰 조회에 성공했습니다.", tokenBalance));
	}

	// 주문 가능 금액 조회
	@GetMapping("/balance/{walletId}")
	public ResponseEntity<ApiResponse<BigDecimal>> selectAvailableBalance(@PathVariable Long walletId) {
		BigDecimal cashBalance = orderService.selectAvailableBalance(walletId);
		if (cashBalance == null) {
			return ResponseEntity.badRequest().body(ApiResponse.error("주문 가능 금액 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("주문 가능 금액 조회에 성공했습니다.", cashBalance));
	}

	// 매수/매도 주문
	@PostMapping
	public ResponseEntity<ApiResponse<Void>> placeOrder(@RequestBody OrderRequestDTO orderDTO) {
		boolean result = orderService.placeOrder(orderDTO);
		if (result) {
			return ResponseEntity.ok(ApiResponse.error("주문이 완료되었습니다."));
		}
		return ResponseEntity.badRequest().body(ApiResponse.error("주문에 실패했습니다."));
	}

	// 주문 취소
	@PostMapping("/cancel/{tokenId}/{orderId}")
	public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long tokenId, @PathVariable Long orderId) {
		boolean isCancelled = orderService.cancelOrder(tokenId, orderId);

		if (isCancelled) {
			return ResponseEntity.ok(ApiResponse.error("주문이 취소되었습니다."));
		} else {
			// 이미 취소되었거나 존재하지 않는 주문일 경우
			return ResponseEntity.badRequest().body(ApiResponse.error("주문 취소에 실패하였습니다."));
		}
	}
}
