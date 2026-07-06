package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.neobank.enums.RepaymentStatus;

public class LoanRepaymentDTO {
    private Long id;
    private Long loanAccountId;
    private Integer instalmentNumber;
    private LocalDate dueDate;
    private BigDecimal emiAmount;
    private BigDecimal principalComponent;
    private BigDecimal interestComponent;
    private RepaymentStatus paymentStatus;
    private LocalDateTime paidAt;

    public LoanRepaymentDTO() {}

    public LoanRepaymentDTO(Long id, Long loanAccountId, Integer instalmentNumber, LocalDate dueDate, BigDecimal emiAmount,
                            BigDecimal principalComponent, BigDecimal interestComponent, RepaymentStatus paymentStatus, LocalDateTime paidAt) {
        this.id = id;
        this.loanAccountId = loanAccountId;
        this.instalmentNumber = instalmentNumber;
        this.dueDate = dueDate;
        this.emiAmount = emiAmount;
        this.principalComponent = principalComponent;
        this.interestComponent = interestComponent;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(Long loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public Integer getInstalmentNumber() {
        return instalmentNumber;
    }

    public void setInstalmentNumber(Integer instalmentNumber) {
        this.instalmentNumber = instalmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public void setEmiAmount(BigDecimal emiAmount) {
        this.emiAmount = emiAmount;
    }

    public BigDecimal getPrincipalComponent() {
        return principalComponent;
    }

    public void setPrincipalComponent(BigDecimal principalComponent) {
        this.principalComponent = principalComponent;
    }

    public BigDecimal getInterestComponent() {
        return interestComponent;
    }

    public void setInterestComponent(BigDecimal interestComponent) {
        this.interestComponent = interestComponent;
    }

    public RepaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(RepaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
