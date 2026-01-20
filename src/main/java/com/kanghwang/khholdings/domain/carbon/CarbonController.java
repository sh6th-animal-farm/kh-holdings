package com.kanghwang.khholdings.domain.carbon;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kanghwang.khholdings.domain.carbon.dto.HoldingRequestDTO;

@RestController
@RequestMapping("/api/carbon")
public class CarbonController {

	@Autowired
	private CarbonService carbonService;

	@GetMapping("/{walletId}")
	public List<HoldingRequestDTO> selectTokenIdByWalletId(@PathVariable Long walletId) {
		return carbonService.selectTokenIdByWalletId(walletId);
	}
}
