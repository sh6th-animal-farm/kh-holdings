package com.kanghwang.khholdings.domain.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.order.OrderRepository;
import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

@Service
public class TradeHistoryWorker {

	@Autowired
	OrderRepository orderRepository;

	@StreamListener("trade:stream")
	public void saveHistory(TransactionRequestDTO txrDTO) {
		orderRepository.callPlaceOrderProcedure(txrDTO);
	}


}
