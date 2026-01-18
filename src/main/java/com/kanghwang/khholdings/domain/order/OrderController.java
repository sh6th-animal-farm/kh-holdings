package com.kanghwang.khholdings.domain.order;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/order")
public class OrderController {

	@Autowired
	OrderService orderService;

	@GetMapping("/balance/{walletId}/{tokenId}")
	public BigDecimal selectHoldingTokenBalance(@PathVariable Long walletId,@PathVariable Long tokenId){
		return orderService.selectHoldingTokenBalance(walletId, tokenId);
	}

	@GetMapping("/balance/{walletId}")
	public BigDecimal selectAvailableBalance(@PathVariable Long walletId){
		return orderService.selectAvailableBalance(walletId);
	}

	@PostMapping
	public ResponseEntity<String> placeOrder(@RequestBody OrderRequestDTO dto) {
		orderService.placeOrder(dto);
		return ResponseEntity.ok("주문이 완료되었습니다.");
	}
}
