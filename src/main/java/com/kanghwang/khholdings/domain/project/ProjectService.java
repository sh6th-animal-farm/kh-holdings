// package com.kanghwang.khholdings.domain.project;
//
// import java.math.BigDecimal;
// import java.util.List;
//
// import org.apache.commons.codec.digest.DigestUtils;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
//
// import com.kanghwang.khholdings.domain.project.dto.DividendDTO;
// import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;
//
// @Service
// public class ProjectService {
//
// 	@Autowired
// 	private ProjectRepository projectRepository;
//
// 	@Transactional
// 	public Long applySubscription(Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount) {
//
// 		// 1. Snowflake ID 생성
// 		Long transactionId = SnowflakeIdGenerator.nextId();
//
// 		// 2. 해시값 생성
// 		String hashValue = DigestUtils.sha256Hex(transactionId.toString() + walletId.toString());
//
// 		Long txHistId = projectRepository.applySubscription(transactionId, tokenId, subscriptionId, walletId, amount, hashValue);
//
// 		// 3. 결과 검증
// 		if (txHistId == null) {
// 			throw new RuntimeException("청약 신청에 실패했습니다.");
// 		}
//
// 		return txHistId;
// 	}
//
// 	@Transactional
// 	public boolean cancelSubscription(Long transactionId) {
//
// 		// 1. Snowflake ID 생성
// 		Long newTransactionId = SnowflakeIdGenerator.nextId();
//
// 		// 2. 해시값 생성
// 		String hashValue = DigestUtils.sha256Hex(transactionId.toString());
//
// 		return projectRepository.cancelSubscription(transactionId, newTransactionId, hashValue);
// 	}
//
// 	@Transactional
// 	public boolean resultSubscription(Long transactionId, Long tokenId, Long passPrice, Long passVolume) {
//
// 		// 1. Snowflake ID 생성
// 		Long passTxId = SnowflakeIdGenerator.nextId();
// 		Long failTxId = SnowflakeIdGenerator.nextId();
//
// 		// 2. 해시값 생성
// 		String passHashValue = DigestUtils.sha256Hex(transactionId.toString());
// 		String failHashValue = DigestUtils.sha256Hex(transactionId.toString());
//
// 		return projectRepository.resultSubscription(transactionId, tokenId, passTxId, failTxId, passPrice, passVolume, passHashValue, failHashValue);
// 	}
//
// 	// 배당 스앱샷
// 	public List<DividendDTO> resultSnapshot(Long tokenId) {
// 		return projectRepository.resultSnapshot(tokenId);
// 	}
//
// 	// 배당 정산
// 	public boolean resultDividend(Long tokenId) {
//
// 		Long transactionId = SnowflakeIdGenerator.nextId();
// 		String hashValue = DigestUtils.sha256Hex(transactionId.toString() + walletId.toString());
//
// 		return projectRepository.resultDividend(transactionId, dividendId, walletId, amount, hashValue);
// 	}
//
// 	// 토큰 소각
// 	public boolean closeToken(Long transactionId, Long tokenId) {
// 		return projectRepository.closeToken(transactionId, tokenId);
// 	}
// }
