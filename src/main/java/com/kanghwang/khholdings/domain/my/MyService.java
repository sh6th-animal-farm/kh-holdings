package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;

@Service
public class MyService {

	@Autowired
	private MyRepository myRepository;

	public List<WalletDTO> selectWalletById(Long walletId){
		return myRepository.selectWalletById(walletId);
	}

	public List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page){
		return myRepository.selectTokenByWalletId(walletId, page);
	}
}
