package com.kanghwang.khholdings.domain.order;

import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.codec.SerializationCodec;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeWorker implements CommandLineRunner {

    private final RedissonClient redissonClient;
    private final OrderRepository orderRepository;

    @Override
    public void run(String... args) throws Exception {
        Thread workerThread = new Thread(this::processStream);
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("[TradeWorker] 정산 프로세스 시작");
    }

    private void processStream() {

        RStream<String, Object> stream = redissonClient.getStream("trade:stream:", new SerializationCodec());
        // 처음부터 혹은 최신부터 읽기 설정 가능
        StreamMessageId lastId = StreamMessageId.ALL;

        while (true) {
            try {
                // 새로운 메시지가 올 때까지 최대 1초 대기하며 읽기
                Map<StreamMessageId, Map<String, Object>> messages = stream.read(StreamReadArgs.greaterThan(lastId).count(10));

                for (Map.Entry<StreamMessageId, Map<String, Object>> entry : messages.entrySet()) {
                    Map<String, Object> dataMap = entry.getValue();
                    Object payload = dataMap.get("data");

                    if(payload instanceof TransactionRequestDTO) {
                        // 1. 체결 정산 처리 (Transaction)
                        handleTransaction((TransactionRequestDTO) payload);
                    } else if (payload instanceof Map) {
                        // 2. 취소 및 환불 (Settlement Signal) 처리
                        handleSettlementSignal((Map<String, Object>) payload);
                    }

                    lastId = entry.getKey();
                    stream.remove(lastId);
                }

                Thread.sleep(10); //cpu 과점 방지
            } catch (Exception e) {
                log.error("[TradeWorker] 오류 발생: ", e);
            }
        }
    }

    // [체결] DB 프로시저 호출
    private void handleTransaction(TransactionRequestDTO txDTO) {
        try {
            //이건 ID값 뭘 가져다 써야 되나....
            log.info("[TradeWorker] 체결 정산 시작: TradeID {}", txDTO.getTradeId());
            orderRepository.p_process_transaction_hists(txDTO);
        } catch (Exception e) {
            log.error("[TradeWorker] 체결 DB 정산 실패: TradeID {}, Error: {}", txDTO.getTradeId(), e.getMessage());
        }
    }

    // [환불/취소] DB 프로시저 호출
    private void handleSettlementSignal(Map<String, Object> signal) {
        try {
            Long txId = (Long) signal.get("txId");
            Long orderId = (Long) signal.get("orderId");
            BigDecimal remainingCash = (BigDecimal) signal.get("remainingCash");
            BigDecimal remainingToken = (BigDecimal) signal.get("remainingToken");
            String type = (String) signal.get("type");

            log.info("[TradeWorker] {} 신호 처리: OrderID {}", type, orderId);
            orderRepository.p_cancel_order_and_refund(txId, orderId, remainingCash, remainingToken);
        } catch (Exception e) {
            log.error("[TradeWorker] 환불/취소 DB 처리 실패: {}", e.getMessage());
        }
    }
}
