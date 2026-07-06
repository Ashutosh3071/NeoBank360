package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanPayoffForecastDTO {
	private Long loanAccountId;
	private String productName;
	private int monthsRemaining;
	private LocalDate projectedPayoffDate;
	private BigDecimal outstandingPrincipal;
	private BigDecimal emiAmount;

	public LoanPayoffForecastDTO() {}

	public LoanPayoffForecastDTO(Long loanAccountId, String productName, int monthsRemaining, LocalDate projectedPayoffDate, BigDecimal outstandingPrincipal, BigDecimal emiAmount) {
		this.loanAccountId = loanAccountId;
		this.productName = productName;
		this.monthsRemaining = monthsRemaining;
		this.projectedPayoffDate = projectedPayoffDate;
		this.outstandingPrincipal = outstandingPrincipal;
		this.emiAmount = emiAmount;
	}

	public Long getLoanAccountId() { return loanAccountId; }
	public void setLoanAccountId(Long loanAccountId) { this.loanAccountId = loanAccountId; }
	public String getProductName() { return productName; }
	public void setProductName(String productName) { this.productName = productName; }
	public int getMonthsRemaining() { return monthsRemaining; }
	public void setMonthsRemaining(int monthsRemaining) { this.monthsRemaining = monthsRemaining; }
	public LocalDate getProjectedPayoffDate() { return projectedPayoffDate; }
	public void setProjectedPayoffDate(LocalDate projectedPayoffDate) { this.projectedPayoffDate = projectedPayoffDate; }
	public BigDecimal getOutstandingPrincipal() { return outstandingPrincipal; }
	public void setOutstandingPrincipal(BigDecimal outstandingPrincipal) { this.outstandingPrincipal = outstandingPrincipal; }
	public BigDecimal getEmiAmount() { return emiAmount; }
	public void setEmiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; }
}
