package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.project.dto.BurnDTO;
import com.kanghwang.khholdings.domain.project.dto.CancelDTO;
import com.kanghwang.khholdings.domain.project.dto.DividendDTO;
import com.kanghwang.khholdings.domain.project.dto.DividendRequestDTO;
import com.kanghwang.khholdings.domain.project.dto.OpenDTO;
import com.kanghwang.khholdings.domain.project.dto.SnapshotDTO;
import com.kanghwang.khholdings.domain.project.dto.SubscriptionDTO;
import com.kanghwang.khholdings.domain.project.dto.SubscriptionRequestDTO;
import com.kanghwang.khholdings.global.util.RedisKeyManager;
import com.kanghwang.khholdings.global.util.SnowflakeIdGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final MarketRepository marketRepository;
	private final SnowflakeIdGenerator snowflakeIdGenerator;
	private final RedissonClient redissonClient;
	private final RedisKeyManager redisKeyManager;

	// 청약 신청
	@Transactional
	public Long applySubscription(Long tokenId, Long subscriptionId, Long walletId, BigDecimal amount) {

		// 1. Snowflake ID 생성
		Long transactionId = snowflakeIdGenerator.nextId();

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
	public CancelDTO cancelSubscription(Long transactionId) {

		// 1. Snowflake ID 생성
		Long newTransactionId = snowflakeIdGenerator.nextId();

		// 2. 해시값 생성
		String hashValue = DigestUtils.sha256Hex(transactionId.toString());

		return projectRepository.cancelSubscription(transactionId, newTransactionId, hashValue);
	}

	// 청약 정산 (당첨, 낙첨)
	@Transactional
	public boolean resultSubscription(Long tokenId, List<SubscriptionRequestDTO> subRequestList) {

		if (subRequestList == null || subRequestList.size() == 0) {
			return false;
		}

		int BATCH_SIZE = 1000;
		List<SubscriptionDTO> batchBuffer = new ArrayList<>();

		for (SubscriptionRequestDTO subRequestDTO : subRequestList) {

			// 1. Snowflake ID 생성
			Long passTxId = snowflakeIdGenerator.nextId();
			Long failTxId = snowflakeIdGenerator.nextId();

			// 2. 해시값 생성
			String passHashValue = DigestUtils.sha256Hex(passTxId.toString());
			String failHashValue = DigestUtils.sha256Hex(failTxId.toString());

			// 3. 요청 객체 생성
			SubscriptionDTO subscriptionDTO = SubscriptionDTO.builder()
				.passTxId(passTxId)
				.failTxId(failTxId)
				.subscriptionId(subRequestDTO.getSubscriptionId())
				.tokenId(tokenId)
				.walletId(subRequestDTO.getWalletId())
				.passPrice(subRequestDTO.getPassPrice())
				.passVolume(subRequestDTO.getPassVolume())
				.passHashValue(passHashValue)
				.failHashValue(failHashValue)
				.build();

			batchBuffer.add(subscriptionDTO);

			if (batchBuffer.size() >= BATCH_SIZE) {
				projectRepository.resultSubscription(batchBuffer);
				batchBuffer.clear();
			}
		}

		if (!batchBuffer.isEmpty()) {
			projectRepository.resultSubscription(batchBuffer);
		}

		return true;
	}

	// 배당 스냅샷
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
	public boolean resultDividend(Long tokenId, List<DividendRequestDTO> divRequestList) {

		if (divRequestList == null || divRequestList.size() == 0) {
			return false;
		}

		int BATCH_SIZE = 1000;
		List<DividendDTO> batchBuffer = new ArrayList<>();

		for (DividendRequestDTO dividendRequestDTO : divRequestList) {

			Long transactionId = snowflakeIdGenerator.nextId();
			String hashValue = DigestUtils.sha256Hex(transactionId.toString());

			DividendDTO dividendDTO = DividendDTO.builder()
				.transactionId(transactionId)
				.dividendId(dividendRequestDTO.getDividendId())
				.tokenId(tokenId)
				.walletId(dividendRequestDTO.getWalletId())
				.amount(dividendRequestDTO.getBeforeTaxAmount())
				.fee(dividendRequestDTO.getBeforeTaxAmount().subtract(dividendRequestDTO.getAfterTaxAmount()))
				.hashValue(hashValue)
				.build();

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
		BigDecimal currentPrice = marketRepository.selectLatestTokenPrice(tokenId);
		if (currentPrice == null) {
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

			BigDecimal amount = holder.getTokenBalance();          // 토큰 보유 수량
			BigDecimal cashAmount = amount.multiply(currentPrice); // 시세를 기준으로 환전

			Long txId1 = snowflakeIdGenerator.nextId();
			Long txId2 = snowflakeIdGenerator.nextId();

			BurnDTO burnDTO = BurnDTO.builder()
				.txId1(txId1)
				.txId2(txId2)
				.tokenId(holder.getTokenId())
				.walletId(holder.getWalletId())
				.amount(amount)
				.cashAmount(cashAmount)
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

	// 토큰 발행
	public boolean openToken(OpenDTO openDTO){

		boolean isSaved = projectRepository.openToken(openDTO) > 0;

		if (isSaved) {
			TokenListDTO tokenInfo = TokenListDTO.builder()
				.tokenId(openDTO.getTokenId())
				.tokenName(openDTO.getTokenName())
				.tickerSymbol(openDTO.getTickerSymbol())
				.marketPrice(openDTO.getIssuePrice())
				.dailyTradeVolume(BigDecimal.ZERO)
				.changeRate(BigDecimal.ZERO)
				.build();

			RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getPrefix() + "market:info");
			marketInfoMap.put(tokenInfo.getTokenId(), tokenInfo);
			redissonClient.getTopic(redisKeyManager.getPrefix() + "market:update:topic").publish(tokenInfo);

			RScoredSortedSet<Long> rankingSet = redissonClient.getScoredSortedSet(redisKeyManager.getPrefix() + "market:ranking");
			rankingSet.add(0.0, tokenInfo.getTokenId());

			log.info("[market:info/market:ranking] 새로운 토큰 등록 완료: {}", tokenInfo.getTokenName());
		}

		return isSaved;
	}
}
