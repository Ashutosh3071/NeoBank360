package com.neobank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.FinancialInsightsDTO;
import com.neobank.service.InsightsService;

@RestController
@RequestMapping("/insights")
public class InsightsController {

	@Autowired
	private InsightsService insightsService;

	@GetMapping("/{userId}")
	public ResponseEntity<FinancialInsightsDTO> getInsights(@PathVariable Long userId) {
		FinancialInsightsDTO insights = insightsService.buildInsights(userId);
		return ResponseEntity.ok(insights);
	}
}
