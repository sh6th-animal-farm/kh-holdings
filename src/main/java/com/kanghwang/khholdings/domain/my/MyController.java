package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;

@RestController
@RequestMapping("/api/my")
public class MyController {

	@Autowired
	private MyService myService;

	@GetMapping("/wallet/{walletId}")
	public List<WalletDTO> selectWalletById(@PathVariable Long walletId){
		return myService.selectWalletById(walletId);
	}

	@GetMapping("/token/{walletId}")
	public List<HoldingDTO> selectTokenByWalletId(@PathVariable Long walletId, @RequestParam Integer page){
		return myService.selectTokenByWalletId(walletId, page);
	}
}
