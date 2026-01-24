package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
	public List<SnapshotDTO> resultSnapshot(Long tokenId) {
		return projectRepository.resultSnapshot(tokenId);
	}

	// 배당 정산
	@Transactional
	public boolean resultDividend(DividendDTO dividendDTO) {

		Long transactionId = SnowflakeIdGenerator.nextId();

		dividendDTO.setTransactionId(transactionId);
		dividendDTO.setHashValue(transactionId.toString() + transactionId.toString());

		return projectRepository.resultDividend(dividendDTO);
	}

	// 토큰 소각
	@Transactional
	public boolean burnToken(Long tokenId) {

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
