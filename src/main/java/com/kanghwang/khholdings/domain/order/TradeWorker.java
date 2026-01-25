package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.kanghwang.khholdings.domain.my.dto.TransactionHistDTO;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import org.redisson.api.*;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kanghwang.khholdings.domain.order.dto.RefundRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.SettlementResultDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeWorker implements CommandLineRunner {

    private final RedissonClient redissonClient;
    private final OrderRepository orderRepository;
    private volatile boolean isRunning = true;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1); // 종료 확인을 위한 래치 (1개의 스레드가 끝날 때까지 대기)
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    // 체결 내역 벌크 인서트를 위한 버퍼
    private final Queue<TransactionHistDTO> tradeBuffer = new ConcurrentLinkedQueue<>();

    // 1분 봉 업데이트 용 Lua Script
    private static final String luaScript = """
            local c = redis.call('HMGET', KEYS[1], 'high', 'low', 'close', 'vol')
            local price = tonumber(ARGV[1])
            local vol = tonumber(ARGV[2])

            if not c[1] then
                -- 새 봉 생성
                redis.call('HMSET', KEYS[1], 'open', price, 'high', price, 'low', price, 'close', price, 'vol', vol)
            else
                -- 기존 봉 업데이트
                if price > tonumber(c[1]) then redis.call('HSET', KEYS[1], 'high', price) end
                if price < tonumber(c[2]) then redis.call('HSET', KEYS[1], 'low', price) end
                redis.call('HSET', KEYS[1], 'close', price)
                redis.call('HINCRBYFLOAT', KEYS[1], 'vol', vol)
            end
            redis.call('EXPIRE', KEYS[1], 10800) -- 3시간 TTL
            """;

    @Override
    public void run(String... args) throws Exception {
        Thread workerThread = new Thread(this::processStream);
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("[TradeWorker] 정산 프로세스 작동 시작");
    }

    @PreDestroy
    public void stop() {
        this.isRunning = false;
        log.info("[TradeWorker] 종료 신호를 받았습니다. 현재 처리중인 작업을 마치고 종료합니다.");
        try {
            // 워커 스레드가 latch.countDown()을 호출할 때까지 최대 5초간 대기
            if (!shutdownLatch.await(5, TimeUnit.SECONDS)) {
                log.warn("[TradeWorker] 워커가 5초 내에 종료되지 않아 강제 진행합니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processStream() {

        RStream<String, Object> stream = redissonClient.getStream("trade:stream:", new JsonJacksonCodec(objectMapper));
        StreamMessageId lastId = StreamMessageId.ALL; // 처음부터 혹은 최신부터 읽기 설정 가능

        while (isRunning) {
            try {
                // 새로운 메시지가 올 때까지 최대 1초 대기
                // lastId보다 큰 (즉, 나중에 들어온) 데이터를 최대 10개씩 읽기
                Map<StreamMessageId, Map<String, Object>> messages = stream.read(
                        StreamReadArgs.greaterThan(lastId).count(10).timeout(Duration.ofSeconds(1)));

                // 데이터가 없으면 건너뛰기
                if (messages.isEmpty())
                    continue;

                for (Map.Entry<StreamMessageId, Map<String, Object>> entry : messages.entrySet()) {
                    StreamMessageId currentId = entry.getKey(); // 현재 처리 중인 메시지의 ID
                    Object data = entry.getValue().get("data");
                    try {
                        // [1] 데이터 처리 로직
                        if (data instanceof TransactionRequestDTO) {
                            // 1. 체결 정산 처리
                            TransactionRequestDTO transactionDTO = objectMapper.convertValue(data,
                                    TransactionRequestDTO.class);
                            handleTransaction(transactionDTO);
                        } else if (data instanceof RefundRequestDTO) {
                            // 2. 취소 및 환불 처리
                            RefundRequestDTO refundDTO = objectMapper.convertValue(data, RefundRequestDTO.class);
                            handleRefund(refundDTO);
                        }

                        // [2] 처리가 성공하면 즉시 해당 메시지 삭제
                        stream.remove(currentId);

                        // [3] 다음 읽기 지점을 현재 메시지 이후로 업데이트
                        lastId = currentId;

                        // stream.remove(lastId); // Redis에서 정산 완료한 데이터 제거
                        // lastId = entry.getKey(); // 이후 lastId보다 큰 데이터들을 읽어오기 위해 업데이트
                    } catch (Exception e) {
                        log.error("[TradeWorker] 정산 처리 중 오류 발생: ", e);
                    }
                }
            } catch (Exception e) {
                log.error("[TradeWorker] 오류 발생: ", e);
                break;
            }
        }
        try {
            shutdownLatch.countDown(); // await로 인해 기다리던 래치 풀어주기
            log.info("[TradeWorker] 워커 스레드가 안전하게 루프를 종료했습니다.");
        } catch (Exception e) {
            log.error("[TradeWorker] 종료 정리 중 오류: ", e);
        }
    }

    // [체결]
    private void handleTransaction(TransactionRequestDTO requestDTO) {

        try {
            // 1. [DB] 정산 후 생성된 OUTPUT을 SettlementResultDTO에 담음
            SettlementResultDTO result = new SettlementResultDTO();
            orderRepository.p_process_transaction_settlement(requestDTO, result);

            // 2. [DB] DB 기록 용 1분 봉 제작
            updateRedisCandle(requestDTO);

            // 3. [비동기 기록] 로그 생성 및 버퍼 추가
            enqueueLogs(requestDTO, result);

            log.info("[TradeWorker] 체결 정산 완료: TradeID {}", requestDTO.getTradeId());

        } catch (Exception e) {

            log.error("[TradeWorker] 체결 및 정산 실패: TradeID {}", requestDTO.getTradeId(), e);

        }
    }

    // [DB] 1분 봉 집계 로직
    private void updateRedisCandle(TransactionRequestDTO trade) {

        // 1분 단위로 버킷팅 (ex: 12:05:33 -> 12:05:00
        long minute = trade.getCreatedAt().truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
        String candleKey = "candle:1m:" + trade.getTokenId() + ":" + minute;

        redissonClient.getScript(org.redisson.client.codec.StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                luaScript, // 상단에서 작성했던 거
                RScript.ReturnType.VALUE,
                List.of(candleKey),
                trade.getTargetPrice().toPlainString(),
                trade.getExecutedVolume().toPlainString());

        // 업데이트된 최신 캔들 정보를 읽어서 전파
        // 추후에 트레이딩뷰 차트를 실시간으로 움직이게 함
        Map<String, String> candleMap = redissonClient
                .<String, String>getMap(candleKey, org.redisson.client.codec.StringCodec.INSTANCE)
                .readAllMap();

        if (candleMap != null && !candleMap.isEmpty()) {
            String topicKey = "candle:topic:" + trade.getTokenId();

            // 시간은 프론트엔드에서 한국 시간으로 변경 예정
            CandleDTO liveCandle = CandleDTO.builder()
                    .tokenId(trade.getTokenId())
                    .unit(1)
                    .candleTime(OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(minute), ZoneOffset.UTC))
                    .openingPrice(new BigDecimal(candleMap.get("open")))
                    .highPrice(new BigDecimal(candleMap.get("high")))
                    .lowPrice(new BigDecimal(candleMap.get("low")))
                    .closingPrice(new BigDecimal(candleMap.get("close")))
                    .tradeVolume(new BigDecimal(candleMap.get("vol")))
                    .build();

            // DTO 자체를 Redis Topic으로 발행 (MarketWorker가 받음)
            redissonClient.getTopic(topicKey, new JsonJacksonCodec(objectMapper)).publish(liveCandle);
        }
    }

    // 매 분 5초에 실행
    // 모으는 건 1분, 저장은 5초 뒤
    @Scheduled(cron = "5 * * * * *")
    public void syncCandleToDB() {
        // 1. 방금 마감된 1분 계산
        long lastMinute = OffsetDateTime.now().minusMinutes(1).truncatedTo(ChronoUnit.MINUTES).toEpochSecond();
        String pattern = "candle:1m:*:" + lastMinute;

        try {
            // 2. 해당 시간내의 모든 토큰 키 찾기
            Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(pattern);

            // 3. Redisson Batch 기능을 써서 한꺼번에 읽기 준비
            RBatch batch = redissonClient.createBatch();
            List<String> keyList = new ArrayList<>();

            for (String key : keys) {
                keyList.add(key);
                // "나중에 이 키들 데이터 한꺼번에 읽어올 거야"라고 예약만 함
                batch.getMap(key, org.redisson.client.codec.StringCodec.INSTANCE).readAllMapAsync();
            }

            if (keyList.isEmpty()) {
                return;
            }

            // 4. 한 번의 네트워크 통신으로 모든 데이터 수집
            BatchResult<?> result = batch.execute();
            List<Map<String, String>> allRawData = (List<Map<String, String>>) result.getResponses();

            // 5. 데이터 가공 (String -> BigDecimal & DTO화)
            List<CandleDTO> candleList = new ArrayList<>();
            for (int i = 0; i < keyList.size(); i++) {

                Map<String, String> raw = allRawData.get(i);
                if (raw == null || raw.isEmpty()) continue;

                // 토큰 아이디 추출
                String[] parts = keyList.get(i).split(":");
                Long tokenId = Long.valueOf(parts[2]);

                CandleDTO candle = CandleDTO.builder()
                    .tokenId(tokenId)
                    .unit(1)
                    .candleTime(OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(lastMinute), ZoneOffset.UTC))
                    .openingPrice(new BigDecimal(raw.get("open")))
                    .highPrice(new BigDecimal(raw.get("high")))
                    .lowPrice(new BigDecimal(raw.get("low")))
                    .closingPrice(new BigDecimal(raw.get("close")))
                    .tradeVolume(new BigDecimal(raw.get("vol")))
                    .build();

                candleList.add(candle);
            }

            // 5. DB에 한꺼번에 저장
            if (!candleList.isEmpty()) {
                orderRepository.insertCandlesBatch(candleList);
                log.info("[TradeWorker - Sync] {} 시점의 1분 봉 {}건을 DB로 저장 완료", lastMinute, candleList.size());
            }

        } catch (Exception e) {
            log.error("[TradeWorker - Sync] 1분 봉 DB 동기화 중 오류 발생: ", e);
        }
    }

    // [DB] 체결 내역 생성
    // TransactionRequestDTO 누가 누구랑 얼마에 체결됐는가?
    // SettlementResultDTO 정산 후 잔액이 얼마가 되었는가?
    private void enqueueLogs(TransactionRequestDTO t, SettlementResultDTO r) {
        BigDecimal execAmount = t.getTargetPrice().multiply(t.getExecutedVolume());

        // 매수자 로그 (CASH OUT, TOKEN IN)
        tradeBuffer.add(buildLog(t.getTxId1(), t.getTradeId(), t.getBuyOrderId(),
                t.getBuyWalletId(), "CASH", "OUT",
                execAmount, r.getBuyCashAfter(), r.getBuyRemToken(),
                r.getBuyRemCash(), BigDecimal.ZERO, t.getCreatedAt()));

        tradeBuffer.add(buildLog(t.getTxId2(), t.getTradeId(), t.getBuyOrderId(),
                t.getBuyWalletId(), "TOKEN", "IN",
                t.getExecutedVolume(), r.getBuyTokenAfter(), r.getBuyRemToken(),
                r.getBuyRemCash(), t.getFeeRate().multiply(t.getExecutedVolume()), t.getCreatedAt()));

        // 매도자 로그 (CASH IN, TOKEN OUT)
        tradeBuffer.add(buildLog(t.getTxId3(), t.getTradeId(), t.getSellOrderId(),
                t.getSellWalletId(), "CASH", "IN",
                execAmount, r.getSellCashAfter(), r.getSellRemToken(),
                r.getSellRemCash(),t.getFeeRate().multiply(execAmount), t.getCreatedAt()));

        tradeBuffer.add(buildLog(t.getTxId4(), t.getTradeId(), t.getSellOrderId(),
                t.getSellWalletId(), "TOKEN", "OUT", t.getExecutedVolume(),
                r.getSellTokenAfter(), r.getSellRemToken(), r.getSellRemCash(),
                BigDecimal.ZERO, t.getCreatedAt()));
    }

    // 체결 내역 생성을 위한 builder
    private TransactionHistDTO buildLog(Long txId, Long tradeId, Long orderId, Long walletId, String asset,
            String dir, BigDecimal amt, BigDecimal bal, BigDecimal remVol, BigDecimal remPrice, BigDecimal fee,
            OffsetDateTime time) {
        return TransactionHistDTO.builder()
                .transactionId(txId).tradeId(tradeId).orderId(orderId).walletId(walletId)
                .transactionType("TRADE").assetType(asset).direction(dir).amount(amt)
                .balanceAfter(bal).remainingVolume(remVol).remainingPrice(remPrice)
                .fee(fee)
                .hashValue(txId + "_" + tradeId)
                .createdAt(time).build();
    }

    // [DB] 1초마다 혹은 버퍼가 차면 DB에 한 번에 저장
    @Scheduled(fixedDelay = 1000)
    public void periodFlush() {

        if (tradeBuffer.isEmpty()) {
            return;
        }

        List<TransactionHistDTO> tradeToSave = new ArrayList<>();
        while (!tradeBuffer.isEmpty() && tradeToSave.size() < 1000) {
            tradeToSave.add(tradeBuffer.poll());
        }

        if (!tradeToSave.isEmpty()) {
            orderRepository.bulkInsertTradeHistory(tradeToSave);
            log.info("[DB] {}건의 체결 내역 저장 완료", tradeToSave.size());
        }
    }

    // [환불/취소] DB 프로시저 호출
    private void handleRefund(RefundRequestDTO refundDTO) {
        try {
            orderRepository.p_cancel_order_and_refund(refundDTO);
            log.info("[TradeWorker] 환불/취소 처리 완료: OrderID {}", refundDTO.getOrderId());
        } catch (Exception e) {
            log.error("[TradeWorker] 환불/취소 처리 실패: {}", e.getMessage());
        }
    }
}
