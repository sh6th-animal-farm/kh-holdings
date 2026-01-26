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

@RestController
@RequestMapping("/api/order")
public class OrderController {

	@Autowired
	OrderService orderService;

	// 해당 토큰 보유 수량 조회
	@GetMapping("/balance/{walletId}/{tokenId}")
	public BigDecimal selectHoldingTokenBalance(@PathVariable Long walletId, @PathVariable Long tokenId) {
		return orderService.selectHoldingTokenBalance(walletId, tokenId);
	}

	// 주문 가능 금액 조회
	@GetMapping("/balance/{walletId}")
	public BigDecimal selectAvailableBalance(@PathVariable Long walletId) {
		return orderService.selectAvailableBalance(walletId);
	}

	// 매수/매도 주문
	@PostMapping
	public ResponseEntity<String> placeOrder(@RequestBody OrderRequestDTO orderDTO) {
		orderService.placeOrder(orderDTO);
		return ResponseEntity.ok("주문이 완료되었습니다.");
	}

	// 주문 취소
	@PostMapping("/cancel/{tokenId}/{orderId}")
	public ResponseEntity<String> cancelOrder(@PathVariable Long tokenId, @PathVariable Long orderId) {
		boolean isCancelled = orderService.cancelOrder(tokenId, orderId);

		if (isCancelled) {
			return ResponseEntity.ok("주문이 취소되었습니다.");
		} else {
			// 이미 취소되었거나 존재하지 않는 주문일 경우
			return ResponseEntity.badRequest().body("주문 취소에 실패하였습니다.");
		}
	}
}
