package com.kanghwang.khholdings.domain.market;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;

@RestController
@RequestMapping("/api/market")
public class MarketController {

	@Autowired
	private MarketService marketService;

	@GetMapping()
	public List<MarketDTO> selectAll() {
		return marketService.selectAll();
	}

	@GetMapping("/search")
	public List<MarketDTO> selectBySearch(@RequestParam(required = false) String content) {
		 return marketService.selectBySearch(content);
	}
}
