package com.neobank.dto;

public class RewardAccrualEntryDTO {
	private String month;
	private int points;

	public RewardAccrualEntryDTO() {}

	public RewardAccrualEntryDTO(String month, int points) {
		this.month = month;
		this.points = points;
	}

	public String getMonth() { return month; }
	public void setMonth(String month) { this.month = month; }
	public int getPoints() { return points; }
	public void setPoints(int points) { this.points = points; }
}
