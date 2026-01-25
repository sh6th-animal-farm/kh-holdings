package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;

@RestController
@RequestMapping("/api/carbon")
public class CarbonController {

	@Autowired
	private CarbonService carbonService;

	@GetMapping("/{walletId}")
	public ResponseEntity<ApiResponse<List<HoldingRequestDTO>>> selectTokenIdByWalletId(@PathVariable Long walletId) {

		List<HoldingRequestDTO> list = carbonService.selectTokenIdByWalletId(walletId);

		if (list == null || list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("토큰 보유 수량 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("토큰 보유 수량 조회에 성공했습니다.", list));
	}
}
