package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectRepository {

	Long applySubscription(Long transactionId, Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount, String hashValue);

	boolean cancelSubscription(Long transactionId, Long newTransactionId, String hashValue);

	boolean resultSubscription(Long transactionId, Long tokenId, Long passTransactionId, Long failTransactionId, Long passPrice, Long passVolume, String passHashValue, String failHashValue);

}
