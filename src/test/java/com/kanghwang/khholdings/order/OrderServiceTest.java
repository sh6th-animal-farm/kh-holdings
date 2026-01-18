//package com.kanghwang.khholdings.order;
//
//import com.kanghwang.khholdings.domain.market.MarketRepository;
//import com.kanghwang.khholdings.domain.order.OrderRepository;
//import com.kanghwang.khholdings.domain.order.OrderService;
//import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
//import com.kanghwang.khholdings.domain.order.type.OrderSide;
//import com.kanghwang.khholdings.domain.order.type.OrderType;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.dao.DataAccessException;
//
//import java.math.BigDecimal;
//
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
////@SpringBootTest
////@Transactional
//public class OrderServiceTest {
//
//    @Autowired
//    private OrderService orderService;
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Autowired
//    private MarketRepository marketRepository;
//
//    @Test
//    @DisplayName("정상적인 매수 주문 시 DB에 주문이 생성되고 자산이 동결되어야 한다")
//    void placeOrderTest() {
//
//        Long snowflakeId = 123456789L;
//        OrderRequestDTO request = new OrderRequestDTO();
//        request.setOrderId(snowflakeId);
//        request.setWalletId(1L);
//        request.setTokenId(1L);
//        request.setOrderSide(OrderSide.BUY);
//        request.setOrderType(OrderType.LIMIT);
//        request.setOrderPrice(new BigDecimal("500000"));
//        request.setOrderVolume(new BigDecimal("10"));
//
////        orderRepository.callPlaceOrderProcedure(request);
//
//        // 3. Then: DB에 잘 들어갔는지 검증
////        OrderRequestDTO result = orderRepository.findById(snowflakeId);
////        assertNotNull(result);
////        assertEquals(snowflakeId, result.getOrderId());
////        assertEquals("NEW", result.getOrderState());
////
////        log.info("생성된 주문 상태: {}", result.getOrderState());
//
////        List<> orders = marketRepository.findOrdersByWalletId(1L);
////        assertFalse(orders.isEmpty(), "주문이 DB에 생성되어야 합니다.");
//
//    }
//
//    @Test
//    @DisplayName("잔액 부족 시 프로시저에서 예외를 던지고 데이터가 저장되지 않아야 한다")
//    void placeOrderBalanceTest() {
//
//        OrderRequestDTO request = new OrderRequestDTO();
//        request.setWalletId(999L); // 잔액이 0인 지갑 ID
//        request.setOrderSide(OrderSide.BUY);
//        request.setOrderType(OrderType.LIMIT);
//        request.setOrderPrice(new BigDecimal("500000"));
//        request.setOrderVolume(new BigDecimal("10"));
//
//        // DataAccessException이 발생하는지 확인
//        assertThrows(DataAccessException.class, () -> {
//           orderService.placeOrder(request);
//        });
//
//        // DB: 주문이 생성되지 않았어야 함
////        List<Map<String, Object>> orders = orderRepository.findOrdersByWalletId(testWalletId);
////        assertTrue(orders.isEmpty(), "에러 발생 시 데이터가 없어야 합니다.");
//
//    }
//
//}
