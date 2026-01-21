package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectRepository {

	Long applySubscription(Long transactionId, Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount, String hashValue);
}
