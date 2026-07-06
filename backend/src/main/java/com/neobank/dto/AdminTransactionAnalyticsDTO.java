package com.neobank.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminTransactionAnalyticsDTO {
	private List<Map<String, Object>> dailyVolumes;
	private BigDecimal averageTicketSize;
	private BigDecimal totalInflow;
	private BigDecimal totalOutflow;

	public AdminTransactionAnalyticsDTO() {}

	public AdminTransactionAnalyticsDTO(List<Map<String, Object>> dailyVolumes, BigDecimal averageTicketSize, BigDecimal totalInflow, BigDecimal totalOutflow) {
		this.dailyVolumes = dailyVolumes;
		this.averageTicketSize = averageTicketSize;
		this.totalInflow = totalInflow;
		this.totalOutflow = totalOutflow;
	}

	public List<Map<String, Object>> getDailyVolumes() { return dailyVolumes; }
	public void setDailyVolumes(List<Map<String, Object>> dailyVolumes) { this.dailyVolumes = dailyVolumes; }
	public BigDecimal getAverageTicketSize() { return averageTicketSize; }
	public void setAverageTicketSize(BigDecimal averageTicketSize) { this.averageTicketSize = averageTicketSize; }
	public BigDecimal getTotalInflow() { return totalInflow; }
	public void setTotalInflow(BigDecimal totalInflow) { this.totalInflow = totalInflow; }
	public BigDecimal getTotalOutflow() { return totalOutflow; }
	public void setTotalOutflow(BigDecimal totalOutflow) { this.totalOutflow = totalOutflow; }
}
