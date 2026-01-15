package com.kanghwang.khholdings.domain.market;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;

@RestController
public class MarketController {

	@Autowired
	private MarketService marketService;

	@GetMapping("/api/market")
	public List<MarketDTO> selectAll() {
		return marketService.selectAll();
	}

	@GetMapping("/api/market/search")
	public List<MarketDTO> selectBySearch(@RequestParam(required = false) String content, Model model) {
		 return marketService.selectBySearch(content);
	}
}
