package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.kanghwang.khholdings.domain.my.dto.TransactionHistDTO;
import com.kanghwang.khholdings.domain.order.dto.*;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderRepository {

	// 특정 토큰 보유량 조회
	BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId);

	// 사용 가능한 잔액 조회
	BigDecimal selectAvailableBalance(Long walletId);

	// 주문
	void callPlaceOrderProcedure(OrderRequestDTO orderDTO);

	// 정산
	void p_process_transaction_settlement(TransactionRequestDTO trade, SettlementResultDTO result);

	// 환불
	void p_cancel_order_and_refund(RefundRequestDTO refundDTO);

	// 체결 내역 벌크 인서트
    void bulkInsertTradeHistory(@Param("list") List<TransactionHistDTO> list);

	// 1분봉 데이터 벌크 인서트
	void insertCandlesBatch(@Param("list") List<CandleDTO> list);
}
