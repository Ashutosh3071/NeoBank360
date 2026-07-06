package com.neobank.dto;

import java.time.LocalDateTime;
import java.util.List;

public class UserActivityDTO {

	private List<TransactionResponse> recentTransactions;
	private List<LocalDateTime> loginEvents;

	public UserActivityDTO() {
	}

	public UserActivityDTO(List<TransactionResponse> recentTransactions, List<LocalDateTime> loginEvents) {
		this.recentTransactions = recentTransactions;
		this.loginEvents = loginEvents;
	}

	public List<TransactionResponse> getRecentTransactions() {
		return recentTransactions;
	}

	public void setRecentTransactions(List<TransactionResponse> recentTransactions) {
		this.recentTransactions = recentTransactions;
	}

	public List<LocalDateTime> getLoginEvents() {
		return loginEvents;
	}

	public void setLoginEvents(List<LocalDateTime> loginEvents) {
		this.loginEvents = loginEvents;
	}
}
