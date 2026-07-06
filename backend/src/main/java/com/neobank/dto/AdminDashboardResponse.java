package com.neobank.dto;

public class AdminDashboardResponse {

	private long totalUsers;
	private long totalAdmins;
	private long totalCustomers;
	private long totalAccounts;
	private long totalTransactions;

	public AdminDashboardResponse(long totalUsers, long totalAdmins, long totalCustomers, long totalAccounts,
			long totalTransactions) {
		this.totalUsers = totalUsers;
		this.totalAdmins = totalAdmins;
		this.totalCustomers = totalCustomers;
		this.totalAccounts = totalAccounts;
		this.totalTransactions = totalTransactions;
	}

	public long getTotalUsers() {
		return totalUsers;
	}

	public long getTotalAdmins() {
		return totalAdmins;
	}

	public long getTotalCustomers() {
		return totalCustomers;
	}

	public long getTotalAccounts() {
		return totalAccounts;
	}

	public long getTotalTransactions() {
		return totalTransactions;
	}
}