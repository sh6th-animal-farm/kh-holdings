package com.kanghwang.khholdings.domain.my;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

@Mapper
public interface MyRepository {

	List<WalletDTO> selectWalletById(Long walletId);

	List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page);

	List<TransactionRequestDTO> selectTxHistByWalletId(Long walletId, Integer page);

	Long selectAccount(Long userId);
}
