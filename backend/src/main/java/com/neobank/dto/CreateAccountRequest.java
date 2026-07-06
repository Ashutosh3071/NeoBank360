package com.neobank.dto;

import com.neobank.enums.AccountType;

public class CreateAccountRequest {
	private AccountType accountType;

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

}