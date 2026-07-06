package com.neobank.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "points_balance", nullable = false)
    private Integer pointsBalance;

    @Column(name = "is_premium_card")
    private Boolean isPremiumCard = false;

    @Column(name = "last_login_points_date")
    private LocalDate lastLoginPointsDate;

    @Column(name = "last_budget_points_month")
    private String lastBudgetPointsMonth;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    void prePersist() {
        if (pointsBalance == null) {
            pointsBalance = 0;
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
        if (isPremiumCard == null) {
            isPremiumCard = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public Reward() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(Integer pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public boolean getIsPremiumCard() {
        return isPremiumCard != null && isPremiumCard;
    }

    public void setIsPremiumCard(boolean premiumCard) {
        isPremiumCard = premiumCard;
    }

    public LocalDate getLastLoginPointsDate() {
        return lastLoginPointsDate;
    }

    public void setLastLoginPointsDate(LocalDate lastLoginPointsDate) {
        this.lastLoginPointsDate = lastLoginPointsDate;
    }

    public String getLastBudgetPointsMonth() {
        return lastBudgetPointsMonth;
    }

    public void setLastBudgetPointsMonth(String lastBudgetPointsMonth) {
        this.lastBudgetPointsMonth = lastBudgetPointsMonth;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
