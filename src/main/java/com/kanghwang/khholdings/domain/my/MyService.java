package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

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

	public List<TransactionRequestDTO> selectTxHistByWalletId(Long walletId, Integer page){
		return myRepository.selectTxHistByWalletId(walletId, page);
	}

	public Long selectAccount(Long userId){
		return myRepository.selectAccount(userId);
	}
}
