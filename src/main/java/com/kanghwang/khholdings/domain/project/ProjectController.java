 package com.kanghwang.khholdings.domain.project;

 import java.math.BigDecimal;
 import java.util.List;

 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.http.ResponseEntity;
 import org.springframework.web.bind.annotation.PathVariable;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.RequestBody;
 import org.springframework.web.bind.annotation.RequestMapping;
 import org.springframework.web.bind.annotation.RequestParam;
 import org.springframework.web.bind.annotation.RestController;

 import com.kanghwang.khholdings.domain.project.dto.DividendRequestDTO;
 import com.kanghwang.khholdings.domain.project.dto.OpenDTO;
 import com.kanghwang.khholdings.domain.project.dto.SnapshotDTO;
 import com.kanghwang.khholdings.domain.project.dto.SubscriptionRequestDTO;
 import com.kanghwang.khholdings.global.dto.ApiResponse;

 @RestController
 @RequestMapping("/api/project")
 public class ProjectController {

 	@Autowired
 	private ProjectService projectService;

 	// 청약 신청
 	@PostMapping("/application/{tokenId}")
 	public ResponseEntity<ApiResponse<Long>> applySubscription(@PathVariable Long tokenId,
 											   @RequestParam Long subscriptionId,
 											   @RequestParam Long walletId,
 											   @RequestParam BigDecimal amount) {

 		try {
 			Long txHistId = projectService.applySubscription(tokenId, subscriptionId, walletId, amount);
 			return ResponseEntity.ok(ApiResponse.success("청약 신청이 완료되었습니다.", txHistId));
 		} catch (RuntimeException e) {
 			return ResponseEntity.badRequest().body(ApiResponse.error("청약 신청에 실패했습니다."));
 		}
 	}

 	// 청약 취소
 	@PostMapping("/cancel/{transactionId}")
 	public ResponseEntity<ApiResponse<Void>> cancelSubscription(@PathVariable Long transactionId) {

 		boolean isCancelled = projectService.cancelSubscription(transactionId);

 		if (isCancelled) {
 			return ResponseEntity.ok(ApiResponse.error("청약 취소가 완료되었습니다."));
 		} else {
 			return ResponseEntity.badRequest().body(ApiResponse.error("청약 취소에 실패했습니다."));
 		}

 	}

 	// 청약 정산 (당첨, 낙첨)
 	@PostMapping("/result/{tokenId}")
 	public ResponseEntity<ApiResponse<Void>> resultSubscription(@PathVariable Long tokenId, @RequestBody List<SubscriptionRequestDTO> subRequestList) {

 		boolean isCompleted = projectService.resultSubscription(tokenId, subRequestList);

 		if (isCompleted) {
 			return ResponseEntity.ok(ApiResponse.error("청약 정산이 완료되었습니다."));
 		} else {
 			return ResponseEntity.badRequest().body(ApiResponse.error("청약 정산에 실패했습니다."));
 		}
 	}

 	// 배당 스냅샷
 	@PostMapping("/dividend/before/{tokenId}")
 	public ResponseEntity<ApiResponse<List<SnapshotDTO>>> resultSnapshot(@PathVariable Long tokenId) {

 		List<SnapshotDTO> list = projectService.resultSnapshot(tokenId);

 		if (list == null || list.isEmpty()) {
 			return ResponseEntity.badRequest().body(ApiResponse.error("배당 스냅샷에 실패했습니다."));
 		}
		return ResponseEntity.ok(ApiResponse.success("배당 스냅샷이 완료되었습니다.", list));
 	}

 	// 배당 정산
 	@PostMapping("/dividend/after/{tokenId}")
 	public ResponseEntity<ApiResponse<Void>> resultDividend(@PathVariable Long tokenId, @RequestBody List<DividendRequestDTO> divRequestList) {

 		boolean isCompleted = projectService.resultDividend(tokenId, divRequestList);

 		if (isCompleted) {
 			return ResponseEntity.ok(ApiResponse.error("배당 정산이 완료되었습니다."));
 		} else {
 			return ResponseEntity.badRequest().body(ApiResponse.error("배당 정산에 실패했습니다."));
 		}
 	}

 	// 토큰 소각
 	@PostMapping("/close/{tokenId}")
 	public ResponseEntity<ApiResponse<Void>> burnToken(@PathVariable Long tokenId) {

 		boolean isCompleted = projectService.burnToken(tokenId);

 		if (isCompleted) {
 			return ResponseEntity.ok(ApiResponse.error("토큰 소각 및 정산이 완료되었습니다."));
 		} else {
 			return ResponseEntity.badRequest().body(ApiResponse.error("토큰 소각에 실패했습니다."));
 		}
 	}

	 // 토큰 발행
	 @PostMapping("/open")
	 public ResponseEntity<ApiResponse<Void>> openToken(@RequestBody OpenDTO openDTO) {

		 boolean result = projectService.openToken(openDTO);

		 if (!result) {
			 return ResponseEntity.badRequest().body(ApiResponse.error("토큰 발행에 실패했습니다."));
		 }
		 return ResponseEntity.ok(ApiResponse.error("토큰 발행이 완료되었습니다."));
	 }
 }
