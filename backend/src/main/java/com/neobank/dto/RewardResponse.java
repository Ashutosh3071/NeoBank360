package com.neobank.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RewardResponse {

    private Long id;
    private Long userId;
    private int pointsBalance;

    @JsonProperty("isPremiumCard")
    private boolean isPremiumCard;

    private LocalDateTime lastUpdated;

    public RewardResponse() {
    }

    public RewardResponse(Long id, Long userId, int pointsBalance, boolean isPremiumCard, LocalDateTime lastUpdated) {
        this.id = id;
        this.userId = userId;
        this.pointsBalance = pointsBalance;
        this.isPremiumCard = isPremiumCard;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(int pointsBalance) { this.pointsBalance = pointsBalance; }

    @JsonProperty("isPremiumCard")
    public boolean isPremiumCard() { return isPremiumCard; }
    public void setPremiumCard(boolean premiumCard) { isPremiumCard = premiumCard; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
