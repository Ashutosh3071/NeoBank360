package com.neobank.dto;

import java.math.BigDecimal;

public class AdminDashboardDTO {

	private long totalUsers;
	private long totalActiveUsers;
	private long totalLoans;
	private long pendingApprovals;
	private long totalTransactions;
	private BigDecimal platformSavingsRate;

	public AdminDashboardDTO() {
	}

	public AdminDashboardDTO(long totalUsers, long totalActiveUsers, long totalLoans, long pendingApprovals,
			long totalTransactions, BigDecimal platformSavingsRate) {
		this.totalUsers = totalUsers;
		this.totalActiveUsers = totalActiveUsers;
		this.totalLoans = totalLoans;
		this.pendingApprovals = pendingApprovals;
		this.totalTransactions = totalTransactions;
		this.platformSavingsRate = platformSavingsRate;
	}

	public long getTotalUsers() {
		return totalUsers;
	}

	public void setTotalUsers(long totalUsers) {
		this.totalUsers = totalUsers;
	}

	public long getTotalActiveUsers() {
		return totalActiveUsers;
	}

	public void setTotalActiveUsers(long totalActiveUsers) {
		this.totalActiveUsers = totalActiveUsers;
	}

	public long getTotalLoans() {
		return totalLoans;
	}

	public void setTotalLoans(long totalLoans) {
		this.totalLoans = totalLoans;
	}

	public long getPendingApprovals() {
		return pendingApprovals;
	}

	public void setPendingApprovals(long pendingApprovals) {
		this.pendingApprovals = pendingApprovals;
	}

	public long getTotalTransactions() {
		return totalTransactions;
	}

	public void setTotalTransactions(long totalTransactions) {
		this.totalTransactions = totalTransactions;
	}

	public BigDecimal getPlatformSavingsRate() {
		return platformSavingsRate;
	}

	public void setPlatformSavingsRate(BigDecimal platformSavingsRate) {
		this.platformSavingsRate = platformSavingsRate;
	}
}
