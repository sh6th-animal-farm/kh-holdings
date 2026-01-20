package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

@RestController
@RequestMapping("/api/my")
public class MyController {

	@Autowired
	private MyService myService;

	//특정 계좌 및 지갑 조회
	@GetMapping("/wallet/{walletId}")
	public List<WalletDTO> selectWalletById(@PathVariable Long walletId){
		return myService.selectWalletById(walletId);
	}

	//보유 토큰 조회
	@GetMapping("/token/{walletId}")
	public List<HoldingDTO> selectTokenByWalletId(@PathVariable Long walletId, @RequestParam Integer page){
		return myService.selectTokenByWalletId(walletId, page);
	}

	//나의 거래 내역 조회(청약, 배당, 거래)
	@GetMapping("/transaction/{walletId}")
	public List<TransactionRequestDTO> selectTxHistByWalletId(@PathVariable Long walletId, @RequestParam Integer page){
		return myService.selectTxHistByWalletId(walletId, page);
	}

	//계좌 연동
	@GetMapping("/account/{userId}")
	public Long selectAccount(@PathVariable Long userId){
		return myService.selectAccount(userId);
	}
}
