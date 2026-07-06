package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResponse {

	private String referenceId;
	private String sourceAccountNumber;
	private String destinationAccountNumber;
	private BigDecimal amount;
	private BigDecimal sourceBalanceAfter;
	private BigDecimal destinationBalanceAfter;
	private LocalDateTime transactionDate;
	private Integer pointsEarned;

	public TransferResponse(String referenceId, String sourceAccountNumber, String destinationAccountNumber,
			BigDecimal amount, BigDecimal sourceBalanceAfter, BigDecimal destinationBalanceAfter,
			LocalDateTime transactionDate, Integer pointsEarned) {
		this.referenceId = referenceId;
		this.sourceAccountNumber = sourceAccountNumber;
		this.destinationAccountNumber = destinationAccountNumber;
		this.amount = amount;
		this.sourceBalanceAfter = sourceBalanceAfter;
		this.destinationBalanceAfter = destinationBalanceAfter;
		this.transactionDate = transactionDate;
		this.pointsEarned = pointsEarned;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public String getSourceAccountNumber() {
		return sourceAccountNumber;
	}

	public String getDestinationAccountNumber() {
		return destinationAccountNumber;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getSourceBalanceAfter() {
		return sourceBalanceAfter;
	}

	public BigDecimal getDestinationBalanceAfter() {
		return destinationBalanceAfter;
	}

	public LocalDateTime getTransactionDate() {
		return transactionDate;
	}

	public Integer getPointsEarned() {
		return pointsEarned;
	}
}