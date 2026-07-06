package com.neobank.dto;

import java.util.Map;

public class AdminLoanAnalyticsDTO {
	private Map<String, Map<String, Long>> loanDistribution;
	private long npaCount;
	private double npaRatio;

	public AdminLoanAnalyticsDTO() {}

	public AdminLoanAnalyticsDTO(Map<String, Map<String, Long>> loanDistribution, long npaCount, double npaRatio) {
		this.loanDistribution = loanDistribution;
		this.npaCount = npaCount;
		this.npaRatio = npaRatio;
	}

	public Map<String, Map<String, Long>> getLoanDistribution() { return loanDistribution; }
	public void setLoanDistribution(Map<String, Map<String, Long>> loanDistribution) { this.loanDistribution = loanDistribution; }
	public long getNpaCount() { return npaCount; }
	public void setNpaCount(long npaCount) { this.npaCount = npaCount; }
	public double getNpaRatio() { return npaRatio; }
	public void setNpaRatio(double npaRatio) { this.npaRatio = npaRatio; }
}
