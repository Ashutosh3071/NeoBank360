package com.neobank.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.CategorySpendingDTO;
import com.neobank.dto.WealthAnalyticsDTO;
import com.neobank.service.AnalyticsService;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

	@Autowired
	private AnalyticsService analyticsService;

	@GetMapping("/spending/{userId}")
	public ResponseEntity<List<CategorySpendingDTO>> getSpendingAnalytics(
			@PathVariable Long userId,
			@RequestParam(defaultValue = "6") int months) {
		return ResponseEntity.ok(analyticsService.getSpendingAnalytics(userId, months));
	}

	@GetMapping("/wealth/{userId}")
	public ResponseEntity<WealthAnalyticsDTO> getWealthAnalytics(@PathVariable Long userId) {
		return ResponseEntity.ok(analyticsService.getWealthAnalytics(userId));
	}
}
