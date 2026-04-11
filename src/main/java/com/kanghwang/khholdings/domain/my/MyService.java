package com.kanghwang.khholdings.domain.my;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.MyTransactionHistDTO;
import com.kanghwang.khholdings.domain.my.dto.UserInfoDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.HoldingShortDTO;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class MyService {

	private final MyRepository myRepository;
	private final RedisKeyManager redisKeyManager;
	private final RedissonClient redissonClient;
	private final ObjectMapper objectMapper;

	// 특정 계좌 및 지갑 조회
	public WalletDTO selectWalletById(Long walletId){
		Long start = System.currentTimeMillis();
		WalletDTO data =  myRepository.selectWalletById(walletId);

		// Redis에 자산 정보가 없는 경우 DB에서 조회한 값으로 초기화
		if (data != null) {
			String walletKey = redisKeyManager.getPersonalWalletInfoKey(walletId);
			RMap<String, Object> walletMap = redissonClient.getMap(walletKey, StringCodec.INSTANCE);

			if (walletMap.isEmpty()) {
				Map<String, Object> initData = new HashMap<>();
				initData.put("cash_balance", data.getCashBalance().toPlainString());
				initData.put("frozen_amount", data.getFrozenAmount().toPlainString());
				initData.put("total_purchased_value", data.getTotalPurchasedValue().toPlainString());

				walletMap.putAll(initData);                    // 한 번에 처리
				walletMap.expire(1, TimeUnit.HOURS); // 데이터가 들어온 시점에 TTL 설정 (1시간)
			}

			// Redis에 데이터가 있다면 TTL 연장 (1시간)
			walletMap.expire(1, TimeUnit.HOURS);
		}

		long end = System.currentTimeMillis();
		// log.info("자산 조회까지 걸린 시간: {}ms", end - start);

		return data;
	}

	// 보유 토큰 조회
	public List<HoldingDTO> selectTokenByWalletId(Long walletId, Integer page){
		// 1. DB에서 리스트 조회
		long start = System.currentTimeMillis();
		List<HoldingDTO> list =  myRepository.selectTokenByWalletId(walletId, page); // 'page = 0'으로 전체 조회

		if (list == null || list.isEmpty()) {
			return null;
		}

		// 2. 리턴할 리스트에서 수량이 0인 것들을 필터링
		List<HoldingDTO> filteredList = list.stream()
			.filter(h -> h.getTokenBalance() != null && h.getTokenBalance().compareTo(BigDecimal.ZERO) > 0)
			.toList();

		// 3. Redis 초기화 로직 (필터링된 데이터를 기준으로 작업)
		// Redis에 보유 토큰 정보가 없는 경우 DB에서 조회한 값으로 초기화
		String holdingKey = redisKeyManager.getPersonalHoldingsInfoKey(walletId);
		RMap<String, String> holdingMap = redissonClient.getMap(holdingKey, StringCodec.INSTANCE);

		if (holdingMap.isEmpty()&& !filteredList.isEmpty()) {
			Map<String, String> batchData = new HashMap<>();

			for (HoldingDTO h : filteredList) {
				HoldingShortDTO dto = new HoldingShortDTO(
					h.getTokenName(),
					h.getTickerSymbol(),
					h.getTokenBalance(),
					h.getPurchasedValue()
				);
				try {
					batchData.put(String.valueOf(h.getTokenId()), objectMapper.writeValueAsString(dto));
				} catch (JsonProcessingException e) {
					log.error("초기 데이터 로딩 중 직렬화 실패", e);
				}
			}

			holdingMap.putAll(batchData);                   // 한 번에 모든 데이터 저장
			holdingMap.expire(1, TimeUnit.HOURS); // 데이터가 들어온 시점에 TTL 설정 (1시간)
		} else if (!holdingMap.isEmpty()) {
			holdingMap.expire(1, TimeUnit.HOURS);
		}

		long end = System.currentTimeMillis();
		// log.info("보유 토큰 조회까지 걸린 시간: {}ms", end - start);

		return filteredList;
	}

	// 나의 거래 내역 조회(필터 조회, 기간 조회, 페이징)
	public List<MyTransactionHistDTO> selectMyTransactionHist(Long walletId,
				String category,
				Integer period,
				Integer page){
		Integer offset = (page - 1) * 10; // offset만큼 건너뛰고 10개 조회
		return myRepository.selectMyTransactionHist(walletId, category, period, offset);
	}

	// 계좌 연동
	public Long selectAccount(Long userId){
		return myRepository.selectAccount(userId);
	}

	// 계좌 생성 및 연동
	public Long createAndSelectAccount(Long userId, String username, String role, String type){
		// 1. 사용자 조회
		boolean isExist = myRepository.existsByUserId(userId);

		// 2. 계정 없으면 생성
		if(!isExist){
			// 기업이 아닌 모든 사용자는 USER로 간주 (SYSTEM, ADMIN 등)
			if (!"ENTERPRISE".equals(role)) role = "USER";
			UserInfoDTO userInfo = new UserInfoDTO(userId, username, role, type);
			myRepository.createUser(userInfo);
		}

		// 3. 계좌 생성
		myRepository.createAccount(userId);

		// 4. 새로 생성된 계좌 조회 및 반환
		return myRepository.selectAccount(userId);
	}
}
