package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.MyTransactionHistDTO;
import com.kanghwang.khholdings.domain.my.dto.UserInfoDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;

@Mapper
public interface MyRepository {

	//특정 계좌 및 지갑 조회
	WalletDTO selectWalletById(Long walletId);

	// 보유 토큰 조회(페이징)
	List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page);

	// 거래 내역 조회(필터 조회, 기간 조회, 페이징)
	List<MyTransactionHistDTO> selectMyTransactionHist(Long walletId, String category, Integer period, Integer offset);

	// 사용자 조회
	boolean existsByUserId(Long userId);

	// 계정 생성
	void createUser(UserInfoDTO userInfo);

	// 계좌 생성
	void createAccount(Long userId);

	// 계좌 연동
	Long selectAccount(Long userId);
}
