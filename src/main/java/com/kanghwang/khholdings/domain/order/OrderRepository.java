package com.kanghwang.khholdings.domain.order;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;

@Mapper
public interface OrderRepository {

	BigDecimal selectHoldingTokenBalance(Long walletId, Long tokenId);

	BigDecimal selectAvailableBalance(Long walletId);

	void callPlaceOrderProcedure(long orderId, OrderRequestDTO orderDto);

	void p_process_transaction_hists(
			@Param("p_trade_id") long tradeId,
			@Param("p_buy_order_id") long buyOrderId,
			@Param("p_sell_order_id") long sellOrderId,
			@Param("p_exec_price") BigDecimal execPrice,
			@Param("p_exec_volume") BigDecimal execVolume,
			@Param("p_fee_rate") BigDecimal feeRate
	);

}
