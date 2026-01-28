package com.kanghwang.khholdings.domain.market;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;
import com.kanghwang.khholdings.domain.market.dto.OrderPriceDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;
import com.kanghwang.khholdings.domain.market.dto.TradeDTO;
import com.kanghwang.khholdings.domain.order.dto.CandleDTO;

@Service
public class MarketService {

	@Autowired
	private MarketRepository marketRepository;

	// 종목 전체 조회
	public List<MarketDTO> selectAll() {
		return marketRepository.selectAll();
	}

	// 종목 검색어 조회
	public List<MarketDTO> selectBySearch(String content) {
		return marketRepository.selectBySearch(content);
	}

	// 현재가 조회
	public BigDecimal getCurrentPrice(Long tokenId) { return marketRepository.selectLatestTokenPrice(tokenId); }

	// 매수 호가 조회
	public List<OrderPriceDTO> selectAllOrderBuyPrice(Long tokenId) {
		BigDecimal price = getCurrentPrice(tokenId);
		return marketRepository.selectAllOrderBuyPrice(tokenId, price);
	}

	// 매도 호가 조회
	public List<OrderPriceDTO> selectAllOrderSellPrice(Long tokenId) {
		BigDecimal price = getCurrentPrice(tokenId);
		return marketRepository.selectAllOrderSellPrice(tokenId, price);
	}

	// 체결 조회
	public List<TradeDTO> selectAllTradePrice(Long tokenId) {
		return marketRepository.selectAllTradePrice(tokenId);
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
