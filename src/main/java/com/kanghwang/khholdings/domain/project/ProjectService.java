package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

@Service
public class ProjectService {

	@Autowired
	private ProjectRepository projectRepository;

	public Long applySubscription(Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount) {

		// 1. Snowflake ID 생성
		Long transactionId = SnowflakeIdGenerator.nextId();

		// 2. 해시값 생성
		String hashValue = DigestUtils.sha256Hex(transactionId.toString() + walletId.toString());

		return projectRepository.applySubscription(transactionId, tokenId, subscriptionId, walletId, amount, hashValue);
	}
}
