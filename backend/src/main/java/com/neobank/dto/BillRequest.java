package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillRequest {

    private String billerName;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String category;

    public String getBillerName() { return billerName; }
    public void setBillerName(String billerName) { this.billerName = billerName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
