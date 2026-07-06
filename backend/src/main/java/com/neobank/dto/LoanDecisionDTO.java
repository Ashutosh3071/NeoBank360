package com.neobank.dto;

import com.neobank.enums.LoanStatus;

public class LoanDecisionDTO {
    private LoanStatus decision; // APPROVED or REJECTED
    private String adminRemarks;

    public LoanDecisionDTO() {}

    public LoanDecisionDTO(LoanStatus decision, String adminRemarks) {
        this.decision = decision;
        this.adminRemarks = adminRemarks;
    }

    public LoanStatus getDecision() {
        return decision;
    }

    public void setDecision(LoanStatus decision) {
        this.decision = decision;
    }

    public String getAdminRemarks() {
        return adminRemarks;
    }

    public void setAdminRemarks(String adminRemarks) {
        this.adminRemarks = adminRemarks;
    }
}
