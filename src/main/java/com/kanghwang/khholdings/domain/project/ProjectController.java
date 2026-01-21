package com.kanghwang.khholdings.domain.project;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

	@Autowired
	private ProjectService projectService;

	@GetMapping("/application/{tokenId}")
	public Long applySubscription(@PathVariable Long tokenId,
		@RequestParam Long subscriptionId,
		@RequestParam Long walletId,
		@RequestParam BigDecimal amount) {
		return projectService.applySubscription(tokenId, subscriptionId, walletId, amount);
	}

}
