package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neobank.enums.TransactionType;

public class TransactionResponse {
	private Long id;
	private Long accountId;
	private String accountNumber;
	private TransactionType transactionType;
	private BigDecimal amount;
	private BigDecimal balanceAfter;
	private String description;
	private LocalDateTime transactionDate;

	public TransactionResponse(Long id, Long accountId, String accountNumber, TransactionType transactionType,
			BigDecimal amount, BigDecimal balanceAfter, String description, LocalDateTime transactionDate) {
		this.id = id;
		this.accountId = accountId;
		this.accountNumber = accountNumber;
		this.transactionType = transactionType;
		this.amount = amount;
		this.balanceAfter = balanceAfter;
		this.description = description;
		this.transactionDate = transactionDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

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

	public BigDecimal getBalanceAfter() {
		return balanceAfter;
	}

	public void setBalanceAfter(BigDecimal balanceAfter) {
		this.balanceAfter = balanceAfter;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(LocalDateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

}