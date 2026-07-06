package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BillResponse {

    private Long id;
    private String billerName;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String status;
    private boolean remindMe;
    private LocalDateTime createdAt;
    private String category;
    private Integer pointsEarned;

    public BillResponse() {
    }

    public BillResponse(Long id, String billerName, BigDecimal amount, LocalDate dueDate,
                        String status, boolean remindMe, LocalDateTime createdAt, String category, Integer pointsEarned) {
        this.id = id;
        this.billerName = billerName;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
        this.remindMe = remindMe;
        this.createdAt = createdAt;
        this.category = category;
        this.pointsEarned = pointsEarned;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBillerName() { return billerName; }
    public void setBillerName(String billerName) { this.billerName = billerName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isRemindMe() { return remindMe; }
    public void setRemindMe(boolean remindMe) { this.remindMe = remindMe; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
}
