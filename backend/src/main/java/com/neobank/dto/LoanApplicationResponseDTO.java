package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neobank.enums.LoanStatus;

public class LoanApplicationResponseDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private Long loanProductId;
    private String loanProductName;
    private BigDecimal requestedAmount;
    private Integer requestedTenureMonths;
    private LoanStatus status;
    private String adminRemarks;
    private LocalDateTime appliedAt;
    private LocalDateTime decidedAt;
    private Long disbursementAccountId;
    private String disbursementAccountNumber;

    public LoanApplicationResponseDTO() {}

    public LoanApplicationResponseDTO(Long id, Long userId, String userEmail, String userFullName, Long loanProductId,
                                      String loanProductName, BigDecimal requestedAmount, Integer requestedTenureMonths,
                                      LoanStatus status, String adminRemarks, LocalDateTime appliedAt, LocalDateTime decidedAt) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userFullName = userFullName;
        this.loanProductId = loanProductId;
        this.loanProductName = loanProductName;
        this.requestedAmount = requestedAmount;
        this.requestedTenureMonths = requestedTenureMonths;
        this.status = status;
        this.adminRemarks = adminRemarks;
        this.appliedAt = appliedAt;
        this.decidedAt = decidedAt;
    }

    public Long getDisbursementAccountId() {
        return disbursementAccountId;
    }

    public void setDisbursementAccountId(Long disbursementAccountId) {
        this.disbursementAccountId = disbursementAccountId;
    }

    public String getDisbursementAccountNumber() {
        return disbursementAccountNumber;
    }

    public void setDisbursementAccountNumber(String disbursementAccountNumber) {
        this.disbursementAccountNumber = disbursementAccountNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Long getLoanProductId() {
        return loanProductId;
    }

    public void setLoanProductId(Long loanProductId) {
        this.loanProductId = loanProductId;
    }

    public String getLoanProductName() {
        return loanProductName;
    }

    public void setLoanProductName(String loanProductName) {
        this.loanProductName = loanProductName;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getRequestedTenureMonths() {
        return requestedTenureMonths;
    }

    public void setRequestedTenureMonths(Integer requestedTenureMonths) {
        this.requestedTenureMonths = requestedTenureMonths;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
