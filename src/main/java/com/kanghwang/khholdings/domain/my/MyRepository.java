package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.TxHistsSearchDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

@Mapper
public interface MyRepository {

	//특정 계좌 및 지갑 조회
	WalletDTO selectWalletById(Long walletId);

	// 보유 토큰 조회(페이징)
	List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page);

	// 거래 내역 조회(필터 조회, 기간 조회, 페이징)
	List<TransactionRequestDTO> selectTxHistByWalletId(TxHistsSearchDTO searchDTO);

	// 계좌 연동
	Long selectAccount(Long userId);
}
