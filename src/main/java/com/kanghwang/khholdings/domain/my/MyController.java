package com.kanghwang.khholdings.domain.my;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.TxHistsSearchDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my")
public class MyController {

	@Autowired
	private MyService myService;

	// 특정 계좌 및 지갑 조회
	@GetMapping("/wallet/{walletId}")
	public List<WalletDTO> selectWalletById(@PathVariable Long walletId){
		return myService.selectWalletById(walletId);
	}

	// 보유 토큰 조회
	@GetMapping("/token/{walletId}")
	public List<HoldingDTO> selectTokenByWalletId(@PathVariable Long walletId, @RequestParam Integer page){
		return myService.selectTokenByWalletId(walletId, page);
	}

	// 나의 거래 내역 조회(필터 조회, 기간 조회, 페이징)
	@GetMapping("/transaction")
	public List<TransactionRequestDTO> selectTxHistByWalletId(@ModelAttribute TxHistsSearchDTO searchDTO){
		return myService.selectTxHistByWalletId(searchDTO);
	}

	// 계좌 연동
	@GetMapping("/account/{userId}")
	public Long selectAccount(@PathVariable Long userId){
		return myService.selectAccount(userId);
	}
}
