package com.kanghwang.khholdings.domain.order.event;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final RedisTemplate<String, String> redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        OrderRequestDTO order = event.order();

        String orderIdStr = "ORD-" + order.getOrderId();
        String redisKey = "spot:orderbook:" + order.getOrderSide().toString().toLowerCase() + ":" + order.getTokenId();
        String infoKey = "spot:order:info:" + order.getTokenId();

        log.info("DB 커밋 완료 후 Redis 주문 등록 시작: {}", orderIdStr);

        try {
            double score = order.getOrderPrice().doubleValue();
            if (order.getOrderSide() == OrderSide.BUY) {
                score = -score;
            }
            redisTemplate.opsForZSet().add(redisKey, orderIdStr, score);
        } catch (Exception e) {
            log.error("Redis 등록 중 오류 발생: {}", e.getMessage());
        }
    }
}
