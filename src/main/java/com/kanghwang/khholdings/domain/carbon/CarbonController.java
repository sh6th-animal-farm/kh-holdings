package com.kanghwang.khholdings.domain.carbon;

import java.util.Collections;
import java.util.List;

import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;

@RestController
@RequestMapping("/api/carbon")
public class CarbonController {

	@Autowired
	private CarbonService carbonService;

	// 해당 토큰에 대한 보유량 및 모든 기업 보유량 조회(기업용)
	@GetMapping("/{walletId}")
	public ResponseEntity<ApiResponse<List<HoldingRequestDTO>>> selectTokenIdByWalletId(@PathVariable Long walletId) {

		List<HoldingRequestDTO> list = carbonService.selectTokenIdByWalletId(walletId);

		if (list == null) {
			return ResponseEntity.badRequest().body(ApiResponse.error("지갑 번호가 존재하지 않습니다."));
		}
		if (list.isEmpty()) {
			ResponseEntity.ok(ApiResponse.success("보유한 토큰이 없습니다.", Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResponse.success("보우 토큰 조회에 성공했습니다.", list));
	}
}
