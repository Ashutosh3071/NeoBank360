package com.neobank.dto;

import java.math.BigDecimal;

public class TrendEntryDTO {

	private int year;
	private int month;
	private String monthLabel;
	private BigDecimal totalIncome;
	private BigDecimal totalExpense;

	public TrendEntryDTO() {
	}

	public TrendEntryDTO(int year, int month, BigDecimal totalIncome, BigDecimal totalExpense) {
		this.year = year;
		this.month = month;
		this.totalIncome = totalIncome;
		this.totalExpense = totalExpense;
	}

	public TrendEntryDTO(int year, int month, String monthLabel, BigDecimal totalIncome, BigDecimal totalExpense) {
		this.year = year;
		this.month = month;
		this.monthLabel = monthLabel;
		this.totalIncome = totalIncome;
		this.totalExpense = totalExpense;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public String getMonthLabel() {
		return monthLabel;
	}

	public void setMonthLabel(String monthLabel) {
		this.monthLabel = monthLabel;
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
}
