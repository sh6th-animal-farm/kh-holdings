package com.kanghwang.khholdings.domain.market;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;

@Service
public class MarketService {

	@Autowired
	private MarketRepository marketRepository;

	public List<MarketDTO> selectAll() {
		return marketRepository.selectAll();
	}

	public List<MarketDTO> selectBySearch(String content) {
		return marketRepository.selectBySearch(content);
	}
}
