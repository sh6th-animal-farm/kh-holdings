package com.kanghwang.khholdings.domain.order.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.order.dto.TransactionRequestDTO;

@Service
public class TokenPubSubListener {

	@Autowired
	SimpMessagingTemplate messagingTemplate;

	public void onMessage(TransactionRequestDTO trxDTO) {
		messagingTemplate.convertAndSendToUser("/topic/token/" + trxDTO.getTokenId(), order);
	}
}
