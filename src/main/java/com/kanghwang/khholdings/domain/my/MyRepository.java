package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.my.dto.WalletDTO;

@Mapper
public interface MyRepository {

	public abstract List<WalletDTO> selectWalletById(Long walletId);
}
