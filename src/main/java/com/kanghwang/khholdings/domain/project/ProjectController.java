// package com.kanghwang.khholdings.domain.project;
//
// import java.math.BigDecimal;
// import java.util.List;
// import java.util.Map;
//
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
//
// import com.kanghwang.khholdings.domain.project.dto.DividendDTO;
//
// @RestController
// @RequestMapping("/api/project")
// public class ProjectController {
//
// 	@Autowired
// 	private ProjectService projectService;
//
// 	// 청약 신청
// 	@PostMapping("/application/{tokenId}")
// 	public ResponseEntity<?> applySubscription(@PathVariable Long tokenId,
// 											   @RequestParam Long subscriptionId,
// 											   @RequestParam Long walletId,
// 											   @RequestParam BigDecimal amount) {
//
// 		try {
// 			Long txHistId = projectService.applySubscription(tokenId, subscriptionId, walletId, amount);
// 			return ResponseEntity.ok(Map.of(
// 				"transactionId", txHistId,
// 				"message", "청약 신청이 완료되었습니다."
// 			));
// 		} catch (RuntimeException e) {
// 			// "청약 신청에 실패했습니다."
// 			return ResponseEntity.badRequest().body(Map.of(
// 				"message", e.getMessage()
// 			));
// 		}
// 	}
//
// 	// 청약 취소
// 	@PostMapping("/cancel/{transactionId}")
// 	public ResponseEntity<?> cancelSubscription(@PathVariable Long transactionId) {
//
// 		boolean isCancelled = projectService.cancelSubscription(transactionId);
//
// 		if (isCancelled) {
// 			return ResponseEntity.ok("청약 취소가 완료되었습니다.");
// 		} else {
// 			return ResponseEntity.badRequest().body("청약 취소에 실패했습니다.");
// 		}
//
// 	}
//
// 	// 청약 정산 (당첨, 낙첨)
// 	@PostMapping("/result/{transactionId}")
// 	public ResponseEntity<?> resultSubscription(@PathVariable Long transactionId,
// 												@RequestParam Long tokenId,
// 												@RequestParam Long passPrice,
// 												@RequestParam Long passVolume) {
//
// 		boolean isCompleted = projectService.resultSubscription(transactionId, tokenId, passPrice, passVolume);
//
// 		if (isCompleted) {
// 			return ResponseEntity.ok("청약 정산이 완료되었습니다.");
// 		} else {
// 			return ResponseEntity.badRequest().body("청약 정산에 실패했습니다.");
// 		}
// 	}
//
// 	// 배당 스냅샷
// 	@PostMapping("/dividend/before/{transactionId}")
// 	public ResponseEntity<?> resultSnapshot(@PathVariable Long tokenId) {
//
// 		List<DividendDTO> list = projectService.resultSnapshot(tokenId);
//
// 		if (!list.isEmpty()) {
// 			return ResponseEntity.ok("배당 스냅샷 완료되었습니다.");
// 		} else {
// 			return ResponseEntity.badRequest().body("배당 스냅샷 실패했습니다.");
// 		}
// 	}
//
// 	// 배당 정산
// 	@PostMapping("/dividend/after/{walletId}")
// 	public ResponseEntity<?> resultDividend(@PathVariable Long walletId) {
//
// 		boolean isCompleted = resultDividend.resultSnapshot(walletId);
//
// 		if (isCompleted) {
// 			return ResponseEntity.ok("배당 스냅샷 완료되었습니다.");
// 		} else {
// 			return ResponseEntity.badRequest().body("배당 스냅샷 실패했습니다.");
// 		}
// 	}
//
// 	// 토큰 소각
// 	@PostMapping("/close/{transactionId}")
// 	public ResponseEntity<?> closeToken(@PathVariable Long tokenId) {
//
// 		boolean isCompleted = closeToken.resultSnapshot(tokenId);
//
// 		if (isCompleted) {
// 			return ResponseEntity.ok("배당 스냅샷 완료되었습니다.");
// 		} else {
// 			return ResponseEntity.badRequest().body("배당 스냅샷 실패했습니다.");
// 		}
// 	}
// }
