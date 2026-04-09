package com.kanghwang.khholdings.domain.my;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kanghwang.khholdings.domain.my.dto.HoldingDTO;
import com.kanghwang.khholdings.domain.my.dto.MyTransactionHistDTO;
import com.kanghwang.khholdings.domain.my.dto.UserInfoDTO;
import com.kanghwang.khholdings.domain.my.dto.WalletDTO;
import com.kanghwang.khholdings.domain.order.dto.HoldingShortDTO;
import com.kanghwang.khholdings.global.dto.ApiResponse;
import com.kanghwang.khholdings.global.util.ApiResponseUtil;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my")
public class MyController {

	private final MyService myService;
	private final RedisKeyManager redisKeyManager;
	private final RedissonClient redissonClient;
	private final ObjectMapper objectMapper;

	// 특정 계좌 및 지갑 조회
	@GetMapping("/wallet/{walletId}")
	public ResponseEntity<ApiResponse<WalletDTO>> selectWalletById(@PathVariable Long walletId){
		WalletDTO data = myService.selectWalletById(walletId);

		if (data == null) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.", null);
		}

		// Redis에 자산 정보가 없는 경우 DB에서 조회한 값으로 초기화
		String walletKey = redisKeyManager.getPersonalWalletInfoKey(walletId);
		RMap<String, BigDecimal> walletMap = redissonClient.getMap(walletKey, StringCodec.INSTANCE);

		if (walletMap.isEmpty()) {
			walletMap.put("cash_balance", data.getCashBalance());
			walletMap.put("total_purchased_value", data.getTotalPurchasedValue());

			// 데이터가 들어온 시점에 TTL 설정 (1시간)
			walletMap.expire(1, TimeUnit.HOURS);
		}

		// Redis에 데이터가 있다면 TTL 연장 (1시간)
		walletMap.expire(1, TimeUnit.HOURS);

		return  ApiResponseUtil.ok("지갑 조회에 성공했습니다.", data);
	}

	// 보유 토큰 조회
	@GetMapping("/token/{walletId}")
	public ResponseEntity<ApiResponse<List<HoldingDTO>>> selectTokenByWalletId(@PathVariable Long walletId, @RequestParam Integer page){
		List<HoldingDTO> list = myService.selectTokenByWalletId(walletId, page);

		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		// Redis에 보유 토큰 정보가 없는 경우 DB에서 조회한 값으로 초기화
		String holdingKey = redisKeyManager.getPersonalHoldingsInfoKey(walletId);
		RMap<String, String> holdingMap = redissonClient.getMap(holdingKey, StringCodec.INSTANCE);

		if (holdingMap.isEmpty()) {
			initializeHodlings(walletId);

			// 데이터가 들어온 시점에 TTL 설정 (1시간)
			holdingMap.expire(1, TimeUnit.HOURS);
		}

		// Redis에 데이터가 있다면 TTL 연장 (1시간)
		holdingMap.expire(1, TimeUnit.HOURS);

		return  ApiResponseUtil.ok("보유 토큰 조회에 성공했습니다.", list);
	}

	void initializeHodlings(Long walletId){
		String holdingKey = redisKeyManager.getPersonalHoldingsInfoKey(walletId);
		RMap<String, String> holdingMap = redissonClient.getMap(holdingKey, StringCodec.INSTANCE);

		List<HoldingDTO> list = myService.selectTokenByWalletId(walletId, 0); // 전체 조회

		for (HoldingDTO h : list) {
			HoldingShortDTO dto = new HoldingShortDTO(
				h.getTokenName(),
				h.getTickerSymbol(),
				h.getTokenBalance(),
				h.getPurchasedValue()
			);
			try {
				holdingMap.put(String.valueOf(h.getTokenId()), objectMapper.writeValueAsString(dto));
			} catch (JsonProcessingException e) {
				// log.error("초기 데이터 로딩 중 직렬화 실패", e);
			}
		}
	}

	// 나의 거래 내역 조회 (카테고리, 기간, 페이징)
	@GetMapping("/transaction/{walletId}")
	public ResponseEntity<ApiResponse<List<MyTransactionHistDTO>>> selectMyTransactionHist(
				@PathVariable Long walletId,
				@RequestParam(defaultValue = "TOKEN") String category,
				@RequestParam(defaultValue = "0") Integer period,
				@RequestParam(defaultValue = "1") Integer page){
		List<MyTransactionHistDTO> list = myService.selectMyTransactionHist(walletId, category, period, page);
		if (list.isEmpty()) {
			return ApiResponseUtil.ok("조회된 결과가 없습니다.");
		}

		return  ApiResponseUtil.ok("거래 내역 조회에 성공했습니다.", list);
	}

	// 계좌 연동
	@GetMapping("/account/{userId}")
	public ResponseEntity<ApiResponse<Long>> selectAccount(@PathVariable Long userId){
		Long data = myService.selectAccount(userId);
		if (data == null) {
			return ApiResponseUtil.ok("계좌 연동에 실패했습니다.", null);
		}

		return  ApiResponseUtil.ok("계좌 연동에 성공했습니다.", data);
	}

	// 계좌 생성 및 연동
	@PostMapping("/create-account")
	public ResponseEntity<ApiResponse<Long>> createAndSelectAccount(@RequestBody UserInfoDTO userInfo){
		Long data = myService.createAndSelectAccount(userInfo.getUserId(), userInfo.getUsername(), userInfo.getRole(), userInfo.getType());
		if (data == null) {
			return ApiResponseUtil.ok("계좌 생성에 실패했습니다.", null);
		}

		return  ApiResponseUtil.ok("계좌 연동에 성공했습니다.", data);
	}
}
