package com.neobank.dto;

import java.math.BigDecimal;
import java.util.Map;

public class CategorySpendingDTO {
	private String month;
	private Map<String, BigDecimal> spendingByCategory;

	public CategorySpendingDTO() {}

	public CategorySpendingDTO(String month, Map<String, BigDecimal> spendingByCategory) {
		this.month = month;
		this.spendingByCategory = spendingByCategory;
	}

	public String getMonth() { return month; }
	public void setMonth(String month) { this.month = month; }
	public Map<String, BigDecimal> getSpendingByCategory() { return spendingByCategory; }
	public void setSpendingByCategory(Map<String, BigDecimal> spendingByCategory) { this.spendingByCategory = spendingByCategory; }
}
