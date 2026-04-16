package com.kanghwang.khholdings.domain.market.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.market.dto.CandleDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDownService {

	private final RestTemplate restTemplate;
	private final MarketRepository marketRepository;
	private static final String BINANCE_URL = "https://api.binance.com/api/v3/klines";

	@Transactional
	public void syncCustomScaledCandles(String symbol, Long tokenId, BigDecimal basePrice) {
		long durationMs = 14L * 24 * 60 * 60 * 1000;
		long startTime = System.currentTimeMillis() - durationMs;
		long endTime = System.currentTimeMillis();

		// 1. 기준이 될 바이낸스의 '첫 번째 시가' 가져오기 (List<List<Object>> 구조 대응)
		BigDecimal binanceStartPrice = fetchFirstBinanceOpenPrice(symbol, startTime);

		if (binanceStartPrice == null) {
			log.error("바이낸스 초기 가격을 가져오지 못했습니다.");
			return;
		}

		while (startTime < endTime) {
			String url = UriComponentsBuilder.fromHttpUrl(BINANCE_URL)
				.queryParam("symbol", symbol)
				.queryParam("interval", "5m")
				.queryParam("startTime", startTime)
				.queryParam("limit", 1000)
				.toUriString();

			// 응답 타입을 List.class로 받으면 내부적으로 List<List<Object>>가 됩니다.
			List<List<Object>> rawData = restTemplate.getForObject(url, List.class);

			if (rawData == null || rawData.isEmpty()) break;

			// 2. 변동률 계산 및 DTO 변환
			List<CandleDTO> scaledCandles = rawData.stream()
				.map(row -> convertToScaledDto(row, tokenId, basePrice, binanceStartPrice))
				.collect(Collectors.toList());

			// 3. DB Upsert 실행
			marketRepository.upsertCandles(scaledCandles);

			// 마지막 캔들의 시간을 가져와서 다음 루프 시작점 설정 (get(0)이 시간)
			long lastTime = Long.parseLong(rawData.get(rawData.size() - 1).get(0).toString());
			startTime = lastTime + (5 * 60 * 1000);

			try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}
	}

	private BigDecimal fetchFirstBinanceOpenPrice(String symbol, long startTime) {
		String url = String.format("%s?symbol=%s&interval=5m&startTime=%d&limit=1", BINANCE_URL, symbol, startTime);
		List<List<Object>> res = restTemplate.getForObject(url, List.class);

		// 리스트 내부의 리스트에서 1번 인덱스(시가) 추출
		return (res != null && !res.isEmpty()) ? new BigDecimal(res.get(0).get(1).toString()) : null;
	}

	private CandleDTO convertToScaledDto(List<Object> row, Long tokenId, BigDecimal myBase, BigDecimal bStart) {
		// row.get(인덱스) 방식으로 변경하여 Casting 에러 방지
		BigDecimal bOpen  = new BigDecimal(row.get(1).toString());
		BigDecimal bHigh  = new BigDecimal(row.get(2).toString());
		BigDecimal bLow   = new BigDecimal(row.get(3).toString());
		BigDecimal bClose = new BigDecimal(row.get(4).toString());

		// 스케일링 공식 적용
		Function<BigDecimal, BigDecimal> scale = (val) ->
			val.divide(bStart, 10, RoundingMode.HALF_UP).multiply(myBase);

		return CandleDTO.builder()
			.tokenId(tokenId)
			.unit(5)
			.openingPrice(scale.apply(bOpen))
			.highPrice(scale.apply(bHigh))
			.lowPrice(scale.apply(bLow))
			.closingPrice(scale.apply(bClose))
			.tradeVolume(new BigDecimal(row.get(5).toString()))
			.tradeAmount(new BigDecimal(row.get(7).toString()))
			.candleTime(Long.parseLong(row.get(0).toString()) / 1000)
			.build();
	}
}