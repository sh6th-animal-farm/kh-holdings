package com.kanghwang.khholdings.domain.order.repository;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kanghwang.khholdings.domain.market.dto.CandleDTO;
import com.kanghwang.khholdings.domain.my.dto.TransactionHistDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderInfoDTO;
import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.RefundRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.SettlementResultDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

@Mapper
public interface OrderRepository {

	// 특정 토큰 보유량 조회
	BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId);

	// 사용 가능한 잔액 조회
	BigDecimal selectAvailableBalance(Long walletId);

	// 주문
	void callPlaceOrderProcedure(OrderRequestDTO orderDTO);

	// 정산
	void callUpdateWalletAndHolding(TransactionRequestDTO trade, SettlementResultDTO result);

	// 환불
	void callCancelOrderAndRefund(RefundRequestDTO refundDTO);

	// 체결 내역 벌크 인서트
    void bulkInsertTransactionHists(@Param("list") List<TransactionHistDTO> list);

	// 1분봉 데이터 벌크 인서트
	void insertCandlesBatch(@Param("list") List<CandleDTO> list);

	// 주문 정보 조회
	OrderInfoDTO getOrderInfoById(Long orderId);
}
