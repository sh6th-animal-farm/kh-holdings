package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;
import com.kanghwang.khholdings.domain.carbon.type.UserClass;

@Mapper
public interface CarbonRepository {

	UserClass getUserClass(Long walletId);

	List<HoldingRequestDTO> selectTokenIdByWalletId(Long walletId);
}
