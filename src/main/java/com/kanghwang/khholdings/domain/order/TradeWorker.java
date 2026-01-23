package com.kanghwang.khholdings.domain.order;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.redisson.RedissonShutdownException;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.codec.SerializationCodec;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.order.dto.RefundRequestDTO;
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

    @Override
    public void run(String... args) throws Exception {
        Thread workerThread = new Thread(this::processStream);
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("[TradeWorker] 정산 프로세스 시작");
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

        RStream<String, Object> stream = redissonClient.getStream("trade:stream:", new SerializationCodec());
        StreamMessageId lastId = StreamMessageId.ALL; // 처음부터 혹은 최신부터 읽기 설정 가능

        while (isRunning) {
            try {
                // 새로운 메시지가 올 때까지 최대 1초 대기
                // lastId보다 큰 (즉, 나중에 들어온) 데이터를 최대 10개씩 읽기
                Map<StreamMessageId, Map<String, Object>> messages = stream.read(
                    StreamReadArgs.greaterThan(lastId).count(10).timeout(Duration.ofSeconds(1)));

                // 데이터가 없으면 건너뛰기
                if (messages.isEmpty()) continue;

                for (Map.Entry<StreamMessageId, Map<String, Object>> entry : messages.entrySet()) {
                    Map<String, Object> dataMap = entry.getValue();
                    Object payload = dataMap.get("data");
                    try {
                        if(payload instanceof TransactionRequestDTO) {
                            // 1. 체결 정산 처리
                            handleTransaction((TransactionRequestDTO) payload);
                        } else if (payload instanceof RefundRequestDTO) {
                            // 2. 취소 및 환불 처리
                            handleRefund((RefundRequestDTO) payload);
                        }

                        stream.remove(lastId);   // Redis에서 정산 완료한 데이터 제거
                        lastId = entry.getKey(); // 이후 lastId보다 큰 데이터들을 읽어오기 위해 업데이트
                    } catch (Exception e) {
                        log.error("[TradeWorker] 정산 처리 중 오류 발생: ", e);
                        break;
                    }
                }
            } catch (Exception e) {
                // 앱 종료 중 Redisson이 먼저 꺼져서 에러가 날 수 있음 (정상)
                if (!isRunning && (e instanceof RedissonShutdownException || e.getCause() instanceof RedissonShutdownException)) {
                    log.info("[TradeWorker] 종료 중 Redisson 연결이 먼저 해제됨 (정상)");
                } else {
                    log.error("[TradeWorker] 오류 발생: ", e);
                }
                break; // 루프 탈출
            } finally {
                shutdownLatch.countDown(); // 래치의 await 풀기
                log.info("[TradeWorker] 워커 스레드가 안전하게 루프를 종료했습니다.");
            }
        }
    }

    // [체결] DB 프로시저 호출
    private void handleTransaction(TransactionRequestDTO transactionDTO) {
        try {
            orderRepository.p_process_transaction_hists(transactionDTO);
            log.info("[TradeWorker] 체결 정산 완료: TradeID {}", transactionDTO.getTradeId());
        } catch (Exception e) {
            log.error("[TradeWorker] 체결 정산 실패: TradeID {}, Error: {}", transactionDTO.getTradeId(), e.getMessage());
        }
    }

    // [환불/취소] DB 프로시저 호출
    private void handleRefund(RefundRequestDTO refundDTO) {
        try {
            orderRepository.p_cancel_order_and_refund(refundDTO.getTxId(), refundDTO.getOrderId(), refundDTO.getRemainingCash(), refundDTO.getRemainingToken());
            log.info("[TradeWorker] 환불/취소 처리 완료: OrderID {}", refundDTO.getOrderId());
        } catch (Exception e) {
            log.error("[TradeWorker] 환불/취소 처리 실패: {}", e.getMessage());
        }
    }
}
