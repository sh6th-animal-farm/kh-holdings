package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.MyTransactionHistDTO;
import com.kanghwang.khholdings.domain.my.dto.UserInfoDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;

@Service
public class MyService {

	@Autowired
	private MyRepository myRepository;

	// 특정 계좌 및 지갑 조회
	public WalletDTO selectWalletById(Long walletId){
		return myRepository.selectWalletById(walletId);
	}

	// 보유 토큰 조회
	public List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page){
		return myRepository.selectTokenByWalletId(walletId, page);
	}

	// 나의 거래 내역 조회(필터 조회, 기간 조회, 페이징)
	public List<MyTransactionHistDTO> selectMyTransactionHist(Long walletId,
				String category,
				Integer period,
				Integer page){
		Integer offset = (page - 1) * 10; // offset만큼 건너뛰고 10개 조회
		return myRepository.selectMyTransactionHist(walletId, category, period, offset);
	}

	// 계좌 연동
	public Long selectAccount(Long userId){
		return myRepository.selectAccount(userId);
	}

	// 계좌 생성 및 연동
	public Long createAndSelectAccount(Long userId, String username, String role, String type){
		// 1. 사용자 조회
		boolean isExist = myRepository.existsByUserId(userId);

		// 2. 계정 없으면 생성
		if(!isExist){
			// 기업이 아닌 모든 사용자는 USER로 간주 (SYSTEM, ADMIN 등)
			if (!"ENTERPRISE".equals(role)) role = "USER";
			UserInfoDTO user = new UserInfoDTO(userId, username, role, type);
			myRepository.createUser(user);
		}

		// 3. 계좌 생성
		myRepository.createAccount(userId);

		// 4. 새로 생성된 계좌 조회 및 반환
		return myRepository.selectAccount(userId);
	}
}
