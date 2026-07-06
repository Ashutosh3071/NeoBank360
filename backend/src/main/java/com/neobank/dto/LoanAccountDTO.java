package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanAccountDTO {
    private Long id;
    private Long loanApplicationId;
    private Long userId;
    private String userEmail;
    private String productName;
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private LocalDateTime disbursedAt;

    public LoanAccountDTO() {}

    public LoanAccountDTO(Long id, Long loanApplicationId, Long userId, String userEmail, String productName,
                          BigDecimal principalAmount, BigDecimal annualInterestRate, Integer tenureMonths,
                          BigDecimal emiAmount, LocalDateTime disbursedAt) {
        this.id = id;
        this.loanApplicationId = loanApplicationId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.productName = productName;
        this.principalAmount = principalAmount;
        this.annualInterestRate = annualInterestRate;
        this.tenureMonths = tenureMonths;
        this.emiAmount = emiAmount;
        this.disbursedAt = disbursedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoanApplicationId() {
        return loanApplicationId;
    }

    public void setLoanApplicationId(Long loanApplicationId) {
        this.loanApplicationId = loanApplicationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public Integer getTenureMonths() {
        return tenureMonths;
    }

    public void setTenureMonths(Integer tenureMonths) {
        this.tenureMonths = tenureMonths;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public void setEmiAmount(BigDecimal emiAmount) {
        this.emiAmount = emiAmount;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }
}
