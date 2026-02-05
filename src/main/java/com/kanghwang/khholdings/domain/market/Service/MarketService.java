package com.kanghwang.khholdings.domain.market.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.market.dto.OrderPriceDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;
import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.TradeDTO;
import com.kanghwang.khholdings.domain.market.dto.CandleDTO;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketRepository marketRepository;
    private final RedissonClient redissonClient;
    private final RedisKeyManager redisKeyManager;

    // 종목 전체 조회
    public List<TokenListDTO> selectAll() {

        RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getMarketInfoKey());
        RScoredSortedSet<Long> rankingSet = redissonClient.getScoredSortedSet(redisKeyManager.getMarketRankKey());

        // 1. 랭킹셋에서 거래량 높은 순(desc)으로 토큰 ID들만 먼저 가져옴
        List<Long> sortedIds = new ArrayList<>(rankingSet.valueRangeReversed(0, -1));
        List<TokenListDTO> list;

        if (!sortedIds.isEmpty()) {
            // 2. Redis에 데이터가 있는 경우: ID 순서대로 Map에서 꺼내기 (이미 정렬된 상태 유지)
            Map<Long, TokenListDTO> dataMap = marketInfoMap.getAll(new HashSet<>(sortedIds));
            list = sortedIds.stream()
                    .map(dataMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            // 3. Redis가 비어있는 경우: DB 조회
            list = marketRepository.selectAll();
            if (list != null && !list.isEmpty()) {
                list.forEach(dto -> {
                    if (dto.getDailyTradeVolume() != null) {
                        dto.setDailyTradeVolume(dto.getDailyTradeVolume().setScale(0, RoundingMode.DOWN));
                    }
                    if (dto.getChangeRate() != null) {
                        dto.setChangeRate(dto.getChangeRate().setScale(2, RoundingMode.HALF_UP));
                    }
                    rankingSet.add(dto.getDailyTradeVolume().doubleValue(), dto.getTokenId());
                });

                Map<Long, TokenListDTO> bulkMap = list.stream()
                        .collect(Collectors.toMap(TokenListDTO::getTokenId, dto -> dto));
                marketInfoMap.putAll(bulkMap);

                list.sort((a, b) -> {
                    BigDecimal volA = a.getDailyTradeVolume() != null ? a.getDailyTradeVolume() : BigDecimal.ZERO;
                    BigDecimal volB = b.getDailyTradeVolume() != null ? b.getDailyTradeVolume() : BigDecimal.ZERO;
                    return volB.compareTo(volA);
                });
            }
        }

        return list == null ? new ArrayList<>() : list;
    }

    // 종목 검색어 조회
    public List<TokenListDTO> selectBySearch(String content) {
        return marketRepository.selectBySearch(content);
    }

    // 차트 조회 [1]
    public List<CandleDTO> selectCandles(Long tokenId, int unit, long start, long end) {

        long start3 = System.currentTimeMillis();
        log.info("차트 조회 [1] - 시작");

        String cacheKey = String.format("candle:%s:%d", unit + "m", tokenId); // Redis 캐시 키
        long currentMinute = OffsetDateTime.now().truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
        String liveCandleKey = redisKeyManager.getCandleKey(tokenId, unit, currentMinute); // Redis 실시간 키

        long redisStart = System.currentTimeMillis();
        log.info("차트 조회 - redis 조회");
        // 1. Redis 캐시 조회
        RScoredSortedSet<CandleDTO> zset = redissonClient.getScoredSortedSet(cacheKey);
        List<CandleDTO> resultList = new ArrayList<>(zset.valueRange(start, true, end, true));
        log.info("차트 조회 redis 조회 - 로직 완료까지 걸린 시간: {}ms", (System.currentTimeMillis() - start3));

        // 2. 캐시된 데이터에 없는 범위
        boolean isMissing = false;
        if (resultList.isEmpty()) {
            isMissing = true;
        } else {
            long oldesCandleTime = resultList.get(0).getCandleTime();
            if (oldesCandleTime > start) {
                isMissing = true;
            }
        }

        // 3. 2에서 생긴 부분 병합
        if (isMissing) {
            List<CandleDTO> dbData = synchronizedLoadFromDb(tokenId, unit, start, end, zset);

            Map<Long, CandleDTO> mergedMap = new TreeMap<>();
            for (CandleDTO c : dbData)
                mergedMap.put(c.getCandleTime(), c);
            for (CandleDTO c : resultList)
                mergedMap.put(c.getCandleTime(), c);

            resultList = new ArrayList<>(mergedMap.values());
        }

        // 3. 실시간 캔들 (DB/Redis Cache에 저장되지 않은 캔들) 병합
        CandleDTO liveCandle = fetchLiveCandle(liveCandleKey, tokenId, unit, currentMinute, resultList);
        if (liveCandle != null) {
            if (resultList.isEmpty()) {
                resultList.add(liveCandle);
            } else {
                int lastIdx = resultList.size() - 1;
                CandleDTO last = resultList.get(lastIdx);
                if (last.getCandleTime() == currentMinute) {
                    resultList.set(lastIdx, liveCandle);
                } else if (last.getCandleTime() < currentMinute) {
                    resultList.add(liveCandle);
                }
            }
        }

        log.info("차트 조회 [1] 로직 완료까지 걸린 시간: {}ms", (System.currentTimeMillis() - start3));
        return resultList;
    }

    // 차트 조회 [2] - redis에 없을 시 DB 로드 및 redis에 저장 (redis + db)
    private List<CandleDTO> synchronizedLoadFromDb(Long tokenId, int unit, long start, long end,
            RScoredSortedSet<CandleDTO> zset) {

        long start2 = System.currentTimeMillis();
        log.info("차트 조회 [2]");

        RLock lock = redissonClient.getLock("lock:candles:" + tokenId);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                // 1. Redis 현재 상황 파악
                Collection<CandleDTO> cached = zset.valueRange(start, true, end, true);
                List<CandleDTO> result = new ArrayList<>(cached);

                // 2. 가장 오래된 캐시 데이터 확인
                long oldestCachedTime = result.isEmpty() ? end + 1 : result.get(0).getCandleTime();

                // 3. 만약 요청한 시작 시간(start)보다 캐시된 데이터가 더 미래라면 (과거가 비어있다면)
                if (oldestCachedTime > start) {
                    // DB 조회
                    List<CandleDTO> dbData = marketRepository.selectCandles(tokenId, unit, start, oldestCachedTime - 1);

                    if (dbData.isEmpty()) {
                        return result;
                    }

                    Map<CandleDTO, Double> toCache = new HashMap<>();
                    for (CandleDTO candle : dbData) {
                        toCache.put(candle, (double) candle.getCandleTime());
                    }
                    zset.addAll(toCache);

                    result.addAll(0, dbData);

                }

                return result;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                if (lock.isHeldByCurrentThread())
                    lock.unlock();
            }
        }

        log.info("차트 조회 [2] 로직 완료까지 걸린 시간: {}ms", (System.currentTimeMillis() - start2));

        return new ArrayList<>();
    }

    // 차트 조회 [3] - Redis에서 가져온 캔들 DTO로 변환
    private CandleDTO fetchLiveCandle(String key, Long tokenId, int unit, long time, List<CandleDTO> resultList) {
        Map<String, String> raw = redissonClient
                .<String, String>getMap(key, org.redisson.client.codec.StringCodec.INSTANCE)
                .readAllMap();

        // 거래가 없는 경우
        if (raw == null || raw.isEmpty() || !raw.containsKey("open")) {
            if (resultList != null && !resultList.isEmpty()) {
                CandleDTO lastCandle = resultList.get(resultList.size() - 1);
                return CandleDTO.builder()
                        .tokenId(tokenId).unit(unit).candleTime(time)
                        .openingPrice(lastCandle.getClosingPrice())
                        .highPrice(lastCandle.getClosingPrice())
                        .lowPrice(lastCandle.getClosingPrice())
                        .closingPrice(lastCandle.getClosingPrice())
                        .tradeVolume(BigDecimal.ZERO)
                        .build();
            }
            return null; // 직전 데이터도 없다면 아예 안 그리는 게 맞습니다.
        }

        return CandleDTO.builder()
                .tokenId(tokenId)
                .unit(unit)
                .candleTime(time)
                .openingPrice(new BigDecimal(raw.get("open")))
                .highPrice(new BigDecimal(raw.get("high")))
                .lowPrice(new BigDecimal(raw.get("low")))
                .closingPrice(new BigDecimal(raw.get("close")))
                .tradeVolume(new BigDecimal(raw.get("vol")))
                .build();
    }

    // 미체결 내역 조회
    public List<PendingDTO> selectPending(Long tokenId, Long walletId) {
        return marketRepository.selectPending(tokenId, walletId);
    }

    // 현재가 조회
    public BigDecimal getCurrentPrice(Long tokenId) {
        return marketRepository.selectLatestTokenPrice(tokenId);
    }

    // 매수 호가 조회
    public List<OrderPriceDTO> selectAllOrderBuyPrice(Long tokenId) {
        BigDecimal price = getCurrentPrice(tokenId);
        return marketRepository.selectAllOrderBuyPrice(tokenId, price);
    }

    // 매도 호가 조회
    public List<OrderPriceDTO> selectAllOrderSellPrice(Long tokenId) {
        BigDecimal price = getCurrentPrice(tokenId);
        return marketRepository.selectAllOrderSellPrice(tokenId, price);
    }

    // 체결 조회
    public List<TradeDTO> selectAllTradePrice(Long tokenId) {
        return marketRepository.selectAllTradePrice(tokenId);
    }

    // 특정 토큰 OHLCV 조회
    public TokenListDTO selectTokenOhlcv(Long tokenId) {

        RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap(redisKeyManager.getMarketInfoKey());
        TokenListDTO token = marketInfoMap.get(tokenId);

        if (token == null) {
            token = marketRepository.selectTokenOhlcv(tokenId);

            if (token != null) {
                marketInfoMap.put(tokenId, token);
            }
        }

        return token;
    }
}
