package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.project.dto.DividendDTO;

@Mapper
public interface ProjectRepository {

	Long applySubscription(Long transactionId, Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount, String hashValue);

	boolean cancelSubscription(Long transactionId, Long newTransactionId, String hashValue);

	boolean resultSubscription(Long transactionId, Long tokenId, Long passTransactionId, Long failTransactionId, Long passPrice, Long passVolume, String passHashValue, String failHashValue);

	List<DividendDTO> resultSnapshot(Long tokenId);

	boolean resultDividend(Long transactionId, Long dividendId, Long walletId, BigDecimal amount, String hashValue);

	boolean closeToken(Long transactionId, Long tokenId);

}
