package com.neobank.dto;

import java.math.BigDecimal;

public class LoanProductDTO {
    private Long id;
    private String productName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal annualInterestRate;
    private String allowedTenures;

    public LoanProductDTO() {}

    public LoanProductDTO(Long id, String productName, BigDecimal minAmount, BigDecimal maxAmount, BigDecimal annualInterestRate, String allowedTenures) {
        this.id = id;
        this.productName = productName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.annualInterestRate = annualInterestRate;
        this.allowedTenures = allowedTenures;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public String getAllowedTenures() {
        return allowedTenures;
    }

    public void setAllowedTenures(String allowedTenures) {
        this.allowedTenures = allowedTenures;
    }
}
