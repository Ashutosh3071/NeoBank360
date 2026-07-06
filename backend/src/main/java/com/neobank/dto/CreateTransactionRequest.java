package com.neobank.dto;

import java.math.BigDecimal;

import com.neobank.enums.TransactionType;

public class CreateTransactionRequest {

	private TransactionType transactionType;

	private BigDecimal amount;

	private String description;

	public TransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(TransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}