package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;

@Mapper
public interface CarbonRepository {

	List<HoldingRequestDTO> selectTokenIdByWalletId(Long walletId);
}
