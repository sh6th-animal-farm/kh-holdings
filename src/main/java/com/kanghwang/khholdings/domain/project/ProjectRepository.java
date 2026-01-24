package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;
import java.util.List;

import com.kanghwang.khholdings.domain.project.dto.BurnDTO;
import com.kanghwang.khholdings.domain.project.dto.SnapshotDTO;
import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.project.dto.DividendDTO;

@Mapper
public interface ProjectRepository {

	// 청약 신청
	Long applySubscription(Long transactionId, Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount, String hashValue);

	// 청약 취소
	boolean cancelSubscription(Long transactionId, Long newTransactionId, String hashValue);

	// 청약 정산 (당첨, 낙첨)
	boolean resultSubscription(Long transactionId, Long tokenId, Long passTransactionId, Long failTransactionId, Long passPrice, Long passVolume, String passHashValue, String failHashValue);

	// 배당 스냅샷
	List<SnapshotDTO> resultSnapshot(Long tokenId);

	// 배당 정산
	boolean resultDividend(DividendDTO dividendDTO);

	// 토큰 소각
	void burnTokenBatch(List<BurnDTO> list);

	// 토큰 삭제
	void deleteToken(Long tokenId);
}
