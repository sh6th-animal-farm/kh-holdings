package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;

@Service
public class CarbonService {

	@Autowired
	private CarbonRepository carbonRepository;

	public List<HoldingRequestDTO> selectTokenIdByWalletId(Long walletId) {
		return carbonRepository.selectTokenIdByWalletId(walletId);
	}
}
