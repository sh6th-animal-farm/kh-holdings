package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.TransactionHistDTO;
import com.kanghwang.khholdings.domain.my.dto.TxHistsSearchDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;

@RestController
@RequestMapping("/api/my")
public class MyController {

	@Autowired
	private MyService myService;

	// 특정 계좌 및 지갑 조회
	@GetMapping("/wallet/{walletId}")
	public ResponseEntity<ApiResponse<List<WalletDTO>>> selectWalletById(@PathVariable Long walletId){
		List<WalletDTO> list = myService.selectWalletById(walletId);

		if (list == null || list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("지갑 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("지갑 조회에 성공했습니다.", list));
	}

	// 보유 토큰 조회
	@GetMapping("/token/{walletId}")
	public ResponseEntity<ApiResponse<List<HoldingDTO>>> selectTokenByWalletId(@PathVariable Long walletId, @RequestParam Integer page){
		List<HoldingDTO> list = myService.selectTokenByWalletId(walletId, page);

		if (list == null || list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("보유 토큰 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("보유 토큰 조회에 성공했습니다.", list));
	}

	// 나의 거래 내역 조회(필터 조회, 기간 조회, 페이징)
	@GetMapping("/transaction")
	public ResponseEntity<ApiResponse<List<TransactionHistDTO>>> selectTxHistByWalletId(@ModelAttribute TxHistsSearchDTO searchDTO){
		List<TransactionHistDTO> list = myService.selectTxHistByWalletId(searchDTO);

		if (list == null || list.isEmpty()) {
			return ResponseEntity.badRequest().body(ApiResponse.error("거래 내역 조회에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("거래 내역 조회에 성공했습니다.", list));
	}

	// 계좌 연동
	@GetMapping("/account/{userId}")
	public ResponseEntity<ApiResponse<Long>> selectAccount(@PathVariable Long userId){
		Long walletId = myService.selectAccount(userId);

		if (walletId == null) {
			return ResponseEntity.badRequest().body(ApiResponse.error("계좌 연동에 실패했습니다."));
		}
		return ResponseEntity.ok(ApiResponse.success("거래 내역 조회에 성공했습니다.", walletId));
	}
}
