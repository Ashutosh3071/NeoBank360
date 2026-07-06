package com.neobank.dto;

import java.util.List;

public class WealthAnalyticsDTO {
	private List<NetWorthEntryDTO> netWorthTimeline;
	private List<LoanPayoffForecastDTO> loanPayoffForecast;
	private List<RewardAccrualEntryDTO> rewardAccrualHistory;

	public WealthAnalyticsDTO() {}

	public WealthAnalyticsDTO(List<NetWorthEntryDTO> netWorthTimeline, List<LoanPayoffForecastDTO> loanPayoffForecast, List<RewardAccrualEntryDTO> rewardAccrualHistory) {
		this.netWorthTimeline = netWorthTimeline;
		this.loanPayoffForecast = loanPayoffForecast;
		this.rewardAccrualHistory = rewardAccrualHistory;
	}

	public List<NetWorthEntryDTO> getNetWorthTimeline() { return netWorthTimeline; }
	public void setNetWorthTimeline(List<NetWorthEntryDTO> netWorthTimeline) { this.netWorthTimeline = netWorthTimeline; }
	public List<LoanPayoffForecastDTO> getLoanPayoffForecast() { return loanPayoffForecast; }
	public void setLoanPayoffForecast(List<LoanPayoffForecastDTO> loanPayoffForecast) { this.loanPayoffForecast = loanPayoffForecast; }
	public List<RewardAccrualEntryDTO> getRewardAccrualHistory() { return rewardAccrualHistory; }
	public void setRewardAccrualHistory(List<RewardAccrualEntryDTO> rewardAccrualHistory) { this.rewardAccrualHistory = rewardAccrualHistory; }
}
