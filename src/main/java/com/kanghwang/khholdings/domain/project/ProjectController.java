 package com.kanghwang.khholdings.domain.project;

 import java.math.BigDecimal;
 import java.util.List;
 import java.util.Map;

 import com.kanghwang.khholdings.domain.project.dto.SnapshotDTO;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.*;

 import com.kanghwang.khholdings.domain.project.dto.DividendDTO;

 @RestController
 @RequestMapping("/api/project")
 public class ProjectController {

 	@Autowired
 	private ProjectService projectService;

 	// 청약 신청
 	@PostMapping("/application/{tokenId}")
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

 	// 청약 취소
 	@PostMapping("/cancel/{transactionId}")
 	public ResponseEntity<?> cancelSubscription(@PathVariable Long transactionId) {

 		boolean isCancelled = projectService.cancelSubscription(transactionId);

 		if (isCancelled) {
 			return ResponseEntity.ok("청약 취소가 완료되었습니다.");
 		} else {
 			return ResponseEntity.badRequest().body("청약 취소에 실패했습니다.");
 		}

 	}

 	// 청약 정산 (당첨, 낙첨)
 	@PostMapping("/result/{transactionId}")
 	public ResponseEntity<?> resultSubscription(@PathVariable Long transactionId,
 												@RequestParam Long tokenId,
 												@RequestParam Long passPrice,
 												@RequestParam Long passVolume) {

 		boolean isCompleted = projectService.resultSubscription(transactionId, tokenId, passPrice, passVolume);

 		if (isCompleted) {
 			return ResponseEntity.ok("청약 정산이 완료되었습니다.");
 		} else {
 			return ResponseEntity.badRequest().body("청약 정산에 실패했습니다.");
 		}
 	}

 	// 배당 스냅샷
 	@PostMapping("/dividend/before/{tokenId}")
 	public ResponseEntity<?> resultSnapshot(@PathVariable Long tokenId) {

 		List<SnapshotDTO> list = projectService.resultSnapshot(tokenId);

 		if (list.isEmpty()) {
 			return ResponseEntity.badRequest().body("배당 스냅샷 실패했습니다.");
 		}
		return ResponseEntity.ok(list);
 	}

 	// 배당 정산
 	@PostMapping("/dividend/after/{walletId}")
 	public ResponseEntity<?> resultDividend(@RequestBody List<DividendDTO> divList) {

 		boolean isCompleted = projectService.resultDividend(divList);

 		if (isCompleted) {
 			return ResponseEntity.ok("배당 정산이 완료되었습니다.");
 		} else {
 			return ResponseEntity.badRequest().body("배당 정산에 실패했습니다.");
 		}
 	}

 	// 토큰 소각
 	@PostMapping("/close/{tokenId}")
 	public ResponseEntity<?> burnToken(@PathVariable Long tokenId) {

 		boolean isCompleted = projectService.burnToken(tokenId);

 		if (isCompleted) {
 			return ResponseEntity.ok("토큰 소각 및 정산이 완료되었습니다.");
 		} else {
 			return ResponseEntity.badRequest().body("토큰 소각에 실패했습니다.");
 		}
 	}
 }
