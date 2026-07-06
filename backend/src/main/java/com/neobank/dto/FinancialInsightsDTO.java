package com.neobank.dto;

import java.math.BigDecimal;
import java.util.List;

public class FinancialInsightsDTO {

	private BigDecimal totalIncome;
	private BigDecimal totalExpense;
	private BigDecimal savings;
	private List<TrendEntryDTO> trendSummary;

	public FinancialInsightsDTO() {
	}

	public FinancialInsightsDTO(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal savings,
			List<TrendEntryDTO> trendSummary) {
		this.totalIncome = totalIncome;
		this.totalExpense = totalExpense;
		this.savings = savings;
		this.trendSummary = trendSummary;
	}

	public BigDecimal getTotalIncome() {
		return totalIncome;
	}

	public void setTotalIncome(BigDecimal totalIncome) {
		this.totalIncome = totalIncome;
	}

	public BigDecimal getTotalExpense() {
		return totalExpense;
	}

	public void setTotalExpense(BigDecimal totalExpense) {
		this.totalExpense = totalExpense;
	}

	public BigDecimal getSavings() {
		return savings;
	}

	public void setSavings(BigDecimal savings) {
		this.savings = savings;
	}

	public List<TrendEntryDTO> getTrendSummary() {
		return trendSummary;
	}

	public void setTrendSummary(List<TrendEntryDTO> trendSummary) {
		this.trendSummary = trendSummary;
	}
}
