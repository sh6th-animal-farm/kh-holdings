package com.kanghwang.khholdings.domain.my;

import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.TxHistsSearchDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MyRepository {

	List<WalletDTO> selectWalletById(Long walletId);

	List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page);

	List<TransactionRequestDTO> selectTxHistByWalletId(TxHistsSearchDTO searchDTO);

	Long selectAccount(Long userId);
}
