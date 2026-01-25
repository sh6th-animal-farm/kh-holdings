package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import com.kanghwang.khholdings.domain.carbon.type.UserClass;
import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;

@Mapper
public interface CarbonRepository {

	UserClass getUserClass(Long walletId);

	List<HoldingRequestDTO> selectTokenIdByWalletId(Long walletId);

}