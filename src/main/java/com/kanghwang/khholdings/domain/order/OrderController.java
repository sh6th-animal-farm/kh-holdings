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
	public ResponseEntity<String> orderByCondition(@RequestBody OrderRequestDTO orDTO) {
		orderService.orderByCondition(orDTO);
		return ResponseEntity.ok("주문이 완료되었습니다.");
	}
}
