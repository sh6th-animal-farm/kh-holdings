package com.kanghwang.khholdings.domain.market.Service;

import java.util.List;

import com.kanghwang.khholdings.domain.market.MarketRepository;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;

@Service
public class MarketService {

	@Autowired
	private MarketRepository marketRepository;

	// 종목 전체 조회
	public List<TokenListDTO> selectAll() {
		return marketRepository.selectAll();
	}

	// 종목 검색어 조회
	public List<TokenListDTO> selectBySearch(String content) {
		return marketRepository.selectBySearch(content);
	}

	// 차트 조회
    public List<CandleDTO> selectCandles(Long tokenId, int unit, int limit) {
		return marketRepository.selectCandles(tokenId, unit, limit);
  }
  
	// 미체결 내역 조회
	public List<PendingDTO>	selectPending(Long tokenId, Long walletId) {
		return marketRepository.selectPending(tokenId, walletId);
	}
}
