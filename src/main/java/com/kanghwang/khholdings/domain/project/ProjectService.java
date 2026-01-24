package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.project.dto.BurnDTO;
import com.kanghwang.khholdings.domain.project.dto.SnapshotDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanghwang.khholdings.domain.project.dto.DividendDTO;
import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

@Service
@RequiredArgsConstructor
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final MarketRepository marketRepository;

	// 청약 신청
	@Transactional
	public Long applySubscription(Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount) {

		// 1. Snowflake ID 생성
		Long transactionId = SnowflakeIdGenerator.nextId();

		// 2. 해시값 생성
		String hashValue = DigestUtils.sha256Hex(transactionId.toString() + walletId.toString());

		Long txHistId = projectRepository.applySubscription(transactionId, tokenId, subscriptionId, walletId, amount,
				hashValue);

		// 3. 결과 검증
		if (txHistId == null) {
			throw new RuntimeException("청약 신청에 실패했습니다.");
		}

		return txHistId;
	}

	// 청약 취소
	@Transactional
	public boolean cancelSubscription(Long transactionId) {

		// 1. Snowflake ID 생성
		Long newTransactionId = SnowflakeIdGenerator.nextId();

		// 2. 해시값 생성
		String hashValue = DigestUtils.sha256Hex(transactionId.toString());

		return projectRepository.cancelSubscription(transactionId, newTransactionId, hashValue);
	}

	// 청약 정산 (당첨, 낙첨)
	@Transactional
	public boolean resultSubscription(Long transactionId, Long tokenId, Long passPrice, Long passVolume) {

		// 1. Snowflake ID 생성
		Long passTxId = SnowflakeIdGenerator.nextId();
		Long failTxId = SnowflakeIdGenerator.nextId();

		// 2. 해시값 생성
		String passHashValue = DigestUtils.sha256Hex(transactionId.toString());
		String failHashValue = DigestUtils.sha256Hex(transactionId.toString());

		return projectRepository.resultSubscription(transactionId, tokenId, passTxId, failTxId, passPrice, passVolume,
				passHashValue, failHashValue);
	}

	// 배당 스앱샷
	@Transactional
	public List<SnapshotDTO> resultSnapshot(Long tokenId) {

		List<SnapshotDTO> list = projectRepository.resultSnapshot(tokenId);

		if (list != null && !list.isEmpty()) {
			projectRepository.insertSnapshot(list);
		}

		return list;
	}

	// 배당 정산
	@Transactional
	public boolean resultDividend(List<DividendDTO> divList) {

		if (divList == null || divList.size() == 0) {
			return false;
		}

		int BATCH_SIZE = 1000;
		List<DividendDTO> batchBuffer = new ArrayList<>();

		for (DividendDTO dividendDTO : divList) {

			Long transactionId = SnowflakeIdGenerator.nextId();

			dividendDTO.setTransactionId(transactionId);
			dividendDTO.setHashValue(transactionId.toString() + transactionId.toString());

			batchBuffer.add(dividendDTO);

			if (batchBuffer.size() >= BATCH_SIZE) {
				projectRepository.resultDividend(batchBuffer);
				batchBuffer.clear();
			}
		}

		if (!batchBuffer.isEmpty()) {
			projectRepository.resultDividend(batchBuffer);
		}

		return true;
	}

	// 토큰 소각
	@Transactional
	public boolean burnToken(Long tokenId) {

		// 토큰 존재 여부 확인
		Map<String, Object> tokenStatus = projectRepository.checkTokenStatus(tokenId);

		if (tokenStatus == null) {
			throw new RuntimeException("존재하지 않는 토큰입니다.");
		}

		if (tokenStatus.get("deleted_at") != null) {
			throw new RuntimeException("이미 소각 처리된 토큰입니다.");
		}

		// 1. 단가 조회
		BigDecimal tradePrice = marketRepository.selectLatestTokenPrice(tokenId);
		if (tradePrice == null) {
			throw new RuntimeException("현재 시세를 찾을 수 없습니다.");
		}

		// 2. 대상자 조회
		List<SnapshotDTO> holders = projectRepository.resultSnapshot(tokenId);
		if (holders == null || holders.isEmpty()) {
			throw new RuntimeException("토큰 보유 대상자가 없습니다.");
		}

		// 3. batch 처리
		final int BATCH_SIZE = 1000;
		List<BurnDTO> list = new ArrayList<>(BATCH_SIZE);

		for (SnapshotDTO holder : holders) {

			BigDecimal tokenBalance = holder.getTokenBalance();
			BigDecimal calcCash = tokenBalance.multiply(tradePrice);

			Long txId1 = SnowflakeIdGenerator.nextId();
			Long txId2 = SnowflakeIdGenerator.nextId();

			BurnDTO burnDTO = BurnDTO.builder()
				.walletId(holder.getWalletId())
				.tokenId(holder.getTokenId())
				.txId1(txId1)
				.txId2(txId2)
				.amount(tokenBalance)
				.cashAmount(calcCash)
				.hashValue1(DigestUtils.sha256Hex(txId1.toString()))
				.hashValue2(DigestUtils.sha256Hex(txId2.toString()))
				.build();

			list.add(burnDTO);

			// 배치 실행
			if (list.size() >= BATCH_SIZE) {
				projectRepository.burnTokenBatch(list);
				list.clear();
			}

		}

		// 남은 데이터 처리
		if (!list.isEmpty()) {
			projectRepository.burnTokenBatch(list);
		}

		// 토큰 삭제
		projectRepository.deleteToken(tokenId);

		return true;
	}
}
