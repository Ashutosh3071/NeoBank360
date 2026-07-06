package com.neobank.dto;

import java.math.BigDecimal;

public class NetWorthEntryDTO {
	private String month;
	private BigDecimal totalBalance;
	private BigDecimal outstandingPrincipal;
	private BigDecimal netWorth;

	public NetWorthEntryDTO() {}

	public NetWorthEntryDTO(String month, BigDecimal totalBalance, BigDecimal outstandingPrincipal, BigDecimal netWorth) {
		this.month = month;
		this.totalBalance = totalBalance;
		this.outstandingPrincipal = outstandingPrincipal;
		this.netWorth = netWorth;
	}

	public String getMonth() { return month; }
	public void setMonth(String month) { this.month = month; }
	public BigDecimal getTotalBalance() { return totalBalance; }
	public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
	public BigDecimal getOutstandingPrincipal() { return outstandingPrincipal; }
	public void setOutstandingPrincipal(BigDecimal outstandingPrincipal) { this.outstandingPrincipal = outstandingPrincipal; }
	public BigDecimal getNetWorth() { return netWorth; }
	public void setNetWorth(BigDecimal netWorth) { this.netWorth = netWorth; }
}
