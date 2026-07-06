package com.neobank.dto;

import java.math.BigDecimal;

public class LoanApplicationRequestDTO {
    private Long loanProductId;
    private BigDecimal requestedAmount;
    private Integer requestedTenureMonths;
    private Long disbursementAccountId;

    public LoanApplicationRequestDTO() {}

    public LoanApplicationRequestDTO(Long loanProductId, BigDecimal requestedAmount, Integer requestedTenureMonths, Long disbursementAccountId) {
        this.loanProductId = loanProductId;
        this.requestedAmount = requestedAmount;
        this.requestedTenureMonths = requestedTenureMonths;
        this.disbursementAccountId = disbursementAccountId;
    }

    public Long getDisbursementAccountId() {
        return disbursementAccountId;
    }

    public void setDisbursementAccountId(Long disbursementAccountId) {
        this.disbursementAccountId = disbursementAccountId;
    }

    public Long getLoanProductId() {
        return loanProductId;
    }

    public void setLoanProductId(Long loanProductId) {
        this.loanProductId = loanProductId;
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
}
