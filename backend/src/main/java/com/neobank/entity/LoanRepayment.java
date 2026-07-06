package com.neobank.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.neobank.enums.RepaymentStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "loan_repayments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "loan_account_id", "instalment_number" })
})
public class LoanRepayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    private LoanAccount loanAccount;

    @Column(name = "instalment_number", nullable = false)
    private Integer instalmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "emi_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "principal_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestComponent;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private RepaymentStatus paymentStatus;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    void prePersist() {
        if (paymentStatus == null) {
            paymentStatus = RepaymentStatus.PENDING;
        }
    }

    public LoanRepayment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
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
