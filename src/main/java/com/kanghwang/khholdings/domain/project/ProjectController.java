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

 import com.kanghwang.khholdings.domain.project.dto.CancelDTO;
 import com.kanghwang.khholdings.domain.project.dto.DividendRequestDTO;
 import com.kanghwang.khholdings.domain.project.dto.OpenDTO;
 import com.kanghwang.khholdings.domain.project.dto.SnapshotDTO;
 import com.kanghwang.khholdings.domain.project.dto.SubscriptionRequestDTO;
 import com.kanghwang.khholdings.domain.project.dto.SubscriptionResultDTO;
 import com.kanghwang.khholdings.global.dto.ApiResponse;
 import com.kanghwang.khholdings.global.util.ApiResponseUtil;

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

		 Long data = projectService.applySubscription(tokenId, subscriptionId, walletId, amount);
		 return  ApiResponseUtil.ok("청약 신청이 완료되었습니다.", data);
 	}

 	// 청약 취소
 	@PostMapping("/cancel/{transactionId}")
 	public ResponseEntity<ApiResponse<CancelDTO>> cancelSubscription(@PathVariable Long transactionId) {
 		CancelDTO data = projectService.cancelSubscription(transactionId);
		return ApiResponseUtil.ok("청약 취소가 완료되었습니다.", data);
 	}

 	// 청약 정산 (당첨, 낙첨)
 	@PostMapping("/result/{tokenId}")
 	public ResponseEntity<ApiResponse<List<SubscriptionResultDTO>>> resultSubscription(@PathVariable Long tokenId, @RequestBody List<SubscriptionRequestDTO> subRequestList) {
		List<SubscriptionResultDTO> list = projectService.resultSubscription(tokenId, subRequestList);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("정산할 내역이 존재하지 않습니다.");
		}

		return ApiResponseUtil.ok("배당 정산이 완료되었습니다.", list);
 	}

 	// 배당 스냅샷
 	@PostMapping("/dividend/before/{tokenId}")
 	public ResponseEntity<ApiResponse<List<SnapshotDTO>>> resultSnapshot(@PathVariable Long tokenId) {
 		List<SnapshotDTO> list = projectService.resultSnapshot(tokenId);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("토큰이 이미 소각되었거나 토큰을 보유중인 사용자가 존재하지 않습니다.");
		}

		return  ApiResponseUtil.ok("배당 스냅샷이 완료되었습니다.", list);
 	}

 	// 배당 정산
 	@PostMapping("/dividend/after/{tokenId}")
 	public ResponseEntity<ApiResponse<List<CancelDTO>>> resultDividend(@PathVariable Long tokenId, @RequestBody List<DividendRequestDTO> divRequestList) {
		List<CancelDTO> list = projectService.resultDividend(tokenId, divRequestList);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("정산할 내역이 존재하지 않습니다.");
		}

		return ApiResponseUtil.ok("배당 정산이 완료되었습니다.", null);
 	}

 	// 토큰 소각
 	@PostMapping("/close/{tokenId}")
 	public ResponseEntity<ApiResponse<Void>> burnToken(@PathVariable Long tokenId) {
		projectService.burnToken(tokenId);
		return ApiResponseUtil.ok("토큰 소각 및 정산이 완료되었습니다.", null);
 	}

	 // 토큰 발행
	 @PostMapping("/open")
	 public ResponseEntity<ApiResponse<Void>> openToken(@RequestBody OpenDTO openDTO) {
		projectService.openToken(openDTO);
		return ApiResponseUtil.ok("토큰 발행이 완료되었습니다.", null);
	 }
 }
