package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

	@Autowired
	private ProjectService projectService;

	@GetMapping("/application/{tokenId}")
	public ResponseEntity<?> applySubscription(@PathVariable Long tokenId,
		@RequestParam Long subscriptionId,
		@RequestParam Long walletId,
		@RequestParam BigDecimal amount) {

		try {
			Long txHistId = projectService.applySubscription(tokenId, subscriptionId, walletId, amount);
			return ResponseEntity.ok(Map.of(
				"transactionId", txHistId,
				"message", "청약 신청이 완료되었습니다."
			));
		} catch (RuntimeException e) {
			// "청약 신청에 실패했습니다."
			return ResponseEntity.badRequest().body(Map.of(
				"message", e.getMessage()
			));
		}
	}
}
