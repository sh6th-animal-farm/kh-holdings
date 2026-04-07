package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.MyTransactionHistDTO;
import com.kanghwang.khholdings.domain.my.dto.UserInfoDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;
import com.kanghwang.khholdings.global.util.ApiResponseUtil;

@RestController
@RequestMapping("/api/my")
public class MyController {

	@Autowired
	private MyService myService;

	// 특정 계좌 및 지갑 조회
	@GetMapping("/wallet/{walletId}")
	public ResponseEntity<ApiResponse<WalletDTO>> selectWalletById(@PathVariable Long walletId){
		WalletDTO data = myService.selectWalletById(walletId);
		if (data == null) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.", null);
		}
		return  ApiResponseUtil.ok("지갑 조회에 성공했습니다.", data);
	}

	// 보유 토큰 조회
	@GetMapping("/token/{walletId}")
	public ResponseEntity<ApiResponse<List<HoldingDTO>>> selectTokenByWalletId(@PathVariable Long walletId, @RequestParam Integer page){
		List<HoldingDTO> list = myService.selectTokenByWalletId(walletId, page);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("보유 토큰 조회에 성공했습니다.", list);
	}

	// 나의 거래 내역 조회 (카테고리, 기간, 페이징)
	@GetMapping("/transaction/{walletId}")
	public ResponseEntity<ApiResponse<List<MyTransactionHistDTO>>> selectMyTransactionHist(
				@PathVariable Long walletId,
				@RequestParam(defaultValue = "TOKEN") String category,
				@RequestParam(defaultValue = "0") Integer period,
				@RequestParam(defaultValue = "1") Integer page){
		List<MyTransactionHistDTO> list = myService.selectMyTransactionHist(walletId, category, period, page);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("거래 내역 조회에 성공했습니다.", list);
	}

	// 계좌 연동
	@GetMapping("/account/{userId}")
	public ResponseEntity<ApiResponse<Long>> selectAccount(@PathVariable Long userId){
		Long data = myService.selectAccount(userId);
		if (data == null) {
			return ApiResponseUtil.ok("계좌 연동에 실패했습니다.", null);
		}

		return  ApiResponseUtil.ok("계좌 연동에 성공했습니다.", data);
	}

	// 계좌 생성 및 연동
	@PostMapping("/create-account")
	public ResponseEntity<ApiResponse<Long>> createAndSelectAccount(@RequestBody UserInfoDTO userInfo){
		Long data = myService.createAndSelectAccount(userInfo.getUserId(), userInfo.getUsername(), userInfo.getRole(), userInfo.getType());
		if (data == null) {
			return ApiResponseUtil.ok("계좌 생성에 실패했습니다.", null);
		}

		return  ApiResponseUtil.ok("계좌 연동에 성공했습니다.", data);
	}
}
