package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.project.dto.BurnDTO;
import com.kanghwang.khholdings.domain.project.dto.DividendDTO;
import com.kanghwang.khholdings.domain.project.dto.SnapshotDTO;
import com.kanghwang.khholdings.domain.project.dto.SubscriptionDTO;

@Mapper
public interface ProjectRepository {

	// 청약 신청
	Long applySubscription(Long transactionId, Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount, String hashValue);

	// 청약 취소
	boolean cancelSubscription(Long transactionId, Long newTransactionId, String hashValue);

	// 청약 정산 (당첨, 낙첨)
	boolean resultSubscription(List<SubscriptionDTO> subscriptionDTOList);

	// 배당 스냅샷 (반환용)
	List<SnapshotDTO> resultSnapshot(Long tokenId);

	// 배당 스냅샷 (저장용)
	void insertSnapshot(List<SnapshotDTO> list);

	// 배당 정산
	void resultDividend(List<DividendDTO> dividendDTO);

	// 토큰 소각
	void burnTokenBatch(List<BurnDTO> list);

	// 토큰 삭제
	void deleteToken(Long tokenId);

	// 토큰 존재 여부 확인
	Map<String, Object> checkTokenStatus(Long tokenId);
}
