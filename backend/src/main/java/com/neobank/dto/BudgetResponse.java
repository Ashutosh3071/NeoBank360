package com.neobank.dto;

import java.math.BigDecimal;

public class BudgetResponse {

    private Long id;
    private String category;
    private String budgetMonth;
    private BigDecimal limitAmount;
    private BigDecimal spent;
    private BigDecimal remaining;
    private double utilizationPercentage;

    public BudgetResponse() {
    }

    public BudgetResponse(Long id, String category, String budgetMonth, BigDecimal limitAmount,
                          BigDecimal spent, BigDecimal remaining, double utilizationPercentage) {
        this.id = id;
        this.category = category;
        this.budgetMonth = budgetMonth;
        this.limitAmount = limitAmount;
        this.spent = spent;
        this.remaining = remaining;
        this.utilizationPercentage = utilizationPercentage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(String budgetMonth) { this.budgetMonth = budgetMonth; }

    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }

    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }

    public BigDecimal getRemaining() { return remaining; }
    public void setRemaining(BigDecimal remaining) { this.remaining = remaining; }

    public double getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }
}
