package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

@Service
public class ProjectService {

	@Autowired
	private ProjectRepository projectRepository;

	@Transactional
	public Long applySubscription(Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount) {

		// 1. Snowflake ID 생성
		Long transactionId = SnowflakeIdGenerator.nextId();

		// 2. 해시값 생성
		String hashValue = DigestUtils.sha256Hex(transactionId.toString() + walletId.toString());

		Long txHistId = projectRepository.applySubscription(transactionId, tokenId, subscriptionId, walletId, amount, hashValue);

		// 3. 결과 검증
		if (txHistId == null) {
			throw new RuntimeException("청약 신청에 실패했습니다.");
		}

		return txHistId;
	}

	@Transactional
	public boolean cancelSubscription(Long transactionId) {

		// 1. Snowflake ID 생성
		Long newTransactionId = SnowflakeIdGenerator.nextId();

		// 2. 해시값 생성
		String hashValue = DigestUtils.sha256Hex(transactionId.toString());

		return projectRepository.cancelSubscription(transactionId, newTransactionId, hashValue);
	}
}
