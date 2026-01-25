package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;

@Mapper
public interface CarbonRepository {

	// 지갑 유무 확인
	boolean existsByWalletId(Long walletId);

	// 해당 토큰에 대한 보유량 및 모든 기업 보유량 조회(기업용)
	List<HoldingRequestDTO> selectTokenIdByWalletId(Long walletId);
}
