package com.kanghwang.khholdings.domain.carbon;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;
import com.kanghwang.khholdings.domain.carbon.type.UserClass;

@Service
public class CarbonService {

	@Autowired
	private CarbonRepository carbonRepository;

	public List<HoldingRequestDTO> selectTokenIdByWalletId(Long walletId) {

		// 1. 기업 권한 체크
		UserClass userClass = carbonRepository.getUserClass(walletId);

		if (userClass != UserClass.ENTERPRISE) {
			// 기업이 아니면 빈 리스트 반환
			return Collections.emptyList();
		}

		// 2. 기업일 때만 조회
		return carbonRepository.selectTokenIdByWalletId(walletId);
	}
}
