package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neobank.enums.AccountStatus;
import com.neobank.enums.AccountType;

public class AccountResponse {
	private Long id;
	private String accountNumber;
	private AccountType accountType;
	private BigDecimal balance;
	private Boolean isActive;
	private AccountStatus accountStatus;
	private LocalDateTime createdAt;

	public AccountResponse(Long id, String accountNumber, AccountType accountType, BigDecimal balance,
			AccountStatus accountStatus, Boolean isActive, LocalDateTime createdAt) {
		super();
		this.id = id;
		this.accountNumber = accountNumber;
		this.accountType = accountType;
		this.balance = balance;
		this.accountStatus = accountStatus;
		this.isActive = isActive;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

}