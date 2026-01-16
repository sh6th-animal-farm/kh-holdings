package com.kanghwang.khholdings.domain.order;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.type.OrderSide;

@Service
public class OrderRedisService {

	@Autowired
	RedisTemplate<String,String> redisTemplate;

	public void processMatching(OrderRequestDTO newOrder) {
		// 1. 상대방 호가창 키 설정 (내가 BUY면 SELL 창을 확인)
		// 교정된 코드
		String oppositeSide = (newOrder.getOrderSide() == OrderSide.BUY) ? "SELL" : "BUY";
		String targetKey = "orderbook:" + oppositeSide + ":" + newOrder.getTokenId();

		// 2. 최적의 체결 대상 조회 (Sorted Set 활용)
		// 매수(BUY)는 최저가 매도(SELL)부터, 매도(SELL)는 최고가 매수(BUY)부터 조회
		Set<ZSetOperations.TypedTuple<String>> matchCandidates = (newOrder.getOrderSide() == OrderSide.BUY) ?
			redisTemplate.opsForZSet().rangeWithScores(targetKey, 0, 0) :
			redisTemplate.opsForZSet().reverseRangeWithScores(targetKey, 0, 0);

		if (matchCandidates != null && !matchCandidates.isEmpty()) {
			ZSetOperations.TypedTuple<String> bestMatch = matchCandidates.iterator().next();
			double matchPrice = bestMatch.getScore();
			Long targetOrderId = Long.parseLong(bestMatch.getValue());

			// 3. 가격 조건 확인 (지정가 매수 >= 매도 호가 인지 체크)
			if (isMatchable(newOrder.getOrderSide(), newOrder.getOrderPrice().doubleValue(), matchPrice)) {
				// [체결 로직 실행] -> DB에 체결 정보 전송 및 Redis 호가 삭제
				executeTrade(newOrder, targetOrderId, matchPrice);
				redisTemplate.opsForZSet().remove(targetKey, String.valueOf(targetOrderId));
				return;
			}
		}

		// 4. 체결되지 않은 잔량은 호가창에 등록
		addOrderToOrderbook(newOrder);
	}

	private boolean isMatchable(OrderSide side, double myPrice, double targetPrice) {
		return (side == OrderSide.BUY) ? myPrice >= targetPrice : myPrice <= targetPrice;
	}

	// 3번 로직 내부의 executeTrade 메서드 실제 구현 예시 (에러 해결용)
	private void executeTrade(OrderRequestDTO newOrder, Long targetOrderId, double matchPrice) {
		// 1. 체결 로그 기록 (DB/Kafka)
		// 2. 실제 자산 정산 서비스 호출 (OrderDbService.settleTrade)
		System.out.println("체결 발생! 주문번호: " + newOrder.getOrderId() + " <-> " + targetOrderId);
	}

	// 4번 로직 (잔량 등록) 예시
	private void addOrderToOrderbook(OrderRequestDTO dto) {
		String myKey = "orderbook:" + dto.getOrderSide().name().toLowerCase() + ":" + dto.getTokenId();
		redisTemplate.opsForZSet().add(myKey, String.valueOf(dto.getOrderId()), dto.getOrderPrice().doubleValue());
	}
}
