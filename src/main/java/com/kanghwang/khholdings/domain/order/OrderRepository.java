package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

@Mapper
public interface OrderRepository {

	BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId);

	BigDecimal selectAvailableBalance(Long walletId);

	void callPlaceOrderProcedure(OrderRequestDTO orderDto);

	void p_process_transaction_hists(TransactionRequestDTO transactionDTO);

	void p_cancel_order_and_refund(Long txId, Long orderId, BigDecimal remainingCash, BigDecimal remainingToken);

}
