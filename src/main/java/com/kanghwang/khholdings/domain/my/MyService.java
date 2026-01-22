package com.kanghwang.khholdings.domain.my;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.TxHistsSearchDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MyService {

	@Autowired
	private MyRepository myRepository;

	// 특정 계좌 및 지갑 조회
	public List<WalletDTO> selectWalletById(Long walletId){
		return myRepository.selectWalletById(walletId);
	}

	// 보유 토큰 조회
	public List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page){
		return myRepository.selectTokenByWalletId(walletId, page);
	}

	// 나의 거래 내역 조회(필터 조회, 기간 조회, 페이징)
	public List<TransactionRequestDTO> selectTxHistByWalletId(TxHistsSearchDTO searchDTO){
		return myRepository.selectTxHistByWalletId(searchDTO);
	}

	// 계좌 연동
	public Long selectAccount(Long userId){
		return myRepository.selectAccount(userId);
	}
}
