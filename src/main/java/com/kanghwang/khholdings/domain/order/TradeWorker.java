package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import org.redisson.api.*;
import org.redisson.api.stream.StreamReadArgs;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.Service.MarketDataService;
import com.kanghwang.khholdings.domain.my.dto.TransactionHistDTO;
import com.kanghwang.khholdings.domain.order.dto.RefundRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.SettlementResultDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import com.kanghwang.khholdings.global.util.RedisKeyManager;

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
    private final RedisKeyManager redisKeyManager;
    private final MarketRepository marketRepository;

    // 체결 내역 벌크 인서트를 위한 버퍼
    private final Queue<TransactionHistDTO> transactionBuffer = new ConcurrentLinkedQueue<>();

    @Override
    public void run(String... args) throws Exception {
        initializeMarketInfo(); // Redis 토큰 리스트(마켓) 정보 초기화
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
        log.info("현채 접속 중인 레디스 스트림 키: [{}]", redisKeyManager.getTradeStreamKey());

        RStream<String, Object> stream = redissonClient.getStream(redisKeyManager.getTradeStreamKey());
        StreamMessageId lastId = StreamMessageId.ALL; // 처음부터 혹은 최신부터 읽기 설정 가능

        while (isRunning) {
            try {
                // 새로운 메시지가 올 때까지 최대 1초 대기
                // lastId보다 큰 (즉, 나중에 들어온) 데이터를 최대 10개씩 읽기
                Map<StreamMessageId, Map<String, Object>> messages = stream.read(
                        StreamReadArgs.greaterThan(lastId).count(10).timeout(Duration.ofSeconds(1)));

                // 데이터가 없으면 건너뛰기
                if (messages.isEmpty()) {
                    continue;
                }

                for (Map.Entry<StreamMessageId, Map<String, Object>> entry : messages.entrySet()) {
                    StreamMessageId currentId = entry.getKey(); // 현재 처리 중인 메시지의 ID
                    Object data = entry.getValue().get("data");

                    try {
                        // [1] 데이터 처리 로직
                        if (data instanceof TransactionRequestDTO transactionDTO) {
                            // 1. 체결 정산 처리
                            handleTransaction(transactionDTO);
                        } else if (data instanceof RefundRequestDTO refundDTO) {
                            // 2. 취소 및 환불 처리
                            handleRefund(refundDTO);
                        }

                        // [2] 처리가 성공하면 즉시 해당 메시지 삭제
                        stream.remove(currentId);

                        // [3] 다음 읽기 지점을 현재 메시지 이후로 업데이트
                        lastId = currentId;

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
    private void handleTransaction(TransactionRequestDTO trade) {

        try {
            // 이부분도 하나의 트랜잭션으로 묶어야 함
            // 1. [DB] 정산 후 생성된 OUTPUT을 SettlementResultDTO에 담음
            SettlementResultDTO result = new SettlementResultDTO();
            orderRepository.p_process_transaction_settlement(trade, result);

            // 2. [비동기 기록] 거래 내역 로그 생성 및 버퍼 추가
            enqueueTransactionLogs(trade, result);

            log.info("[TradeWorker] 체결 정산 완료: TradeID {}", trade.getTradeId());

        } catch (Exception e) {

            log.error("[TradeWorker] 체결 및 정산 실패: TradeID {}", trade.getTradeId(), e);

        }
    }

    // [DB] 체결 내역 생성
    // TransactionRequestDTO 누가 누구랑 얼마에 체결됐는가?
    // SettlementResultDTO 정산 후 잔액이 얼마가 되었는가?
    private void enqueueTransactionLogs(TransactionRequestDTO t, SettlementResultDTO r) {
        BigDecimal execAmount = t.getTargetPrice().multiply(t.getExecutedVolume());

        // 매수자 로그 (CASH OUT, TOKEN IN)
        transactionBuffer.add(buildTrnasactionLog(t.getTxId1(), t.getTradeId(), t.getBuyOrderId(),
                t.getBuyWalletId(), "CASH", "OUT",
                execAmount, r.getBuyCashAfter(), r.getBuyRemToken(),
                r.getBuyRemCash(), BigDecimal.ZERO, t.getCreatedAt()));

        transactionBuffer.add(buildTrnasactionLog(t.getTxId2(), t.getTradeId(), t.getBuyOrderId(),
                t.getBuyWalletId(), "TOKEN", "IN",
                t.getExecutedVolume(), r.getBuyTokenAfter(), r.getBuyRemToken(),
                r.getBuyRemCash(), t.getFeeRate().multiply(t.getExecutedVolume()), t.getCreatedAt()));

        // 매도자 로그 (CASH IN, TOKEN OUT)
        transactionBuffer.add(buildTrnasactionLog(t.getTxId3(), t.getTradeId(), t.getSellOrderId(),
                t.getSellWalletId(), "CASH", "IN",
                execAmount, r.getSellCashAfter(), r.getSellRemToken(),
                r.getSellRemCash(),t.getFeeRate().multiply(execAmount), t.getCreatedAt()));

        transactionBuffer.add(buildTrnasactionLog(t.getTxId4(), t.getTradeId(), t.getSellOrderId(),
                t.getSellWalletId(), "TOKEN", "OUT",
                t.getExecutedVolume(), r.getSellTokenAfter(), r.getSellRemToken(),
                r.getSellRemCash(), BigDecimal.ZERO, t.getCreatedAt()));
    }

    // 체결 내역 생성을 위한 builder
    private TransactionHistDTO buildTrnasactionLog(Long txId, Long tradeId, Long orderId, Long walletId, String asset,
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

        if (transactionBuffer.isEmpty()) {
            return;
        }

        List<TransactionHistDTO> transactionToSave = new ArrayList<>();
        while (!transactionBuffer.isEmpty() && transactionToSave.size() < 1000) {
            // 버퍼에서 처리할 체결 내역 로그를 하나씩 꺼내옴 (최대 1000개)
            transactionToSave.add(transactionBuffer.poll());
        }

        if (!transactionToSave.isEmpty()) {
            // 처리할 체결 내역들을 한 번에 처리
            orderRepository.bulkInsertTransactionHists(transactionToSave);
            log.info("[DB] {}건의 체결 내역 저장 완료", transactionToSave.size());
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

    private void initializeMarketInfo() {
        log.info("[TradeWorker] Redis에 토큰 리스트 초기화");
        RMap<Long, TokenListDTO> marketInfoMap = redissonClient.getMap("market:info");
        List<TokenListDTO> tokens = marketRepository.selectAll();

        if(tokens == null || tokens.isEmpty()) {
            log.warn("[TradeWorker] 토큰 데이터가 존재하지 않습니다.");
            return;
        }
        Map<Long, TokenListDTO> bulkMap = tokens.stream()
                .collect(Collectors.toMap(TokenListDTO::getTokenId, dto -> {
                    dto.setChangeRate(calculateRate(dto.getMarketPrice(), dto.getOpenPrice()));
                    return dto;
                }));

        marketInfoMap.putAll(bulkMap);

        RScoredSortedSet<Long> rankingSet = redissonClient.getScoredSortedSet("market:ranking");
        bulkMap.forEach((id, dto) -> {
            rankingSet.add(dto.getDailyTradeVolume().doubleValue(), id);
        });
    }

    private BigDecimal calculateRate(BigDecimal current, BigDecimal open) {
        if(open == null || open.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return current.subtract(open)
                .divide(open, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
