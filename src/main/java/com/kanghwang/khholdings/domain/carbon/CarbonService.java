package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;

@Service
public class CarbonService {

	@Autowired
	private CarbonRepository carbonRepository;

	// 해당 토큰에 대한 보유량 및 모든 기업 보유량 조회(기업용)
	public List<HoldingRequestDTO> selectTokenIdByWalletId(Long walletId) {

		// 지갑 유무 조회
		if (!carbonRepository.existsByWalletId(walletId)) {
			return null;
		}

		return carbonRepository.selectTokenIdByWalletId(walletId);
	}
}
