package com.neobank.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PendingApprovalDTO {

	private Long id;
	private String type; // e.g. "LOAN_APPLICATION" or "ACCOUNT_APPROVAL"
	private String applicantName;
	private String productName;
	private BigDecimal requestedAmount;
	private LocalDateTime appliedAt;

	// ✅ KYC fields shown to admin during account approval review
	private String applicantEmail;
	private String aadhaarNumber;
	private String panNumber;

	public PendingApprovalDTO() {
	}

	public PendingApprovalDTO(Long id, String type, String applicantName, String productName,
			BigDecimal requestedAmount, LocalDateTime appliedAt) {
		this.id = id;
		this.type = type;
		this.applicantName = applicantName;
		this.productName = productName;
		this.requestedAmount = requestedAmount;
		this.appliedAt = appliedAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getApplicantName() {
		return applicantName;
	}

	public void setApplicantName(String applicantName) {
		this.applicantName = applicantName;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public BigDecimal getRequestedAmount() {
		return requestedAmount;
	}

	public void setRequestedAmount(BigDecimal requestedAmount) {
		this.requestedAmount = requestedAmount;
	}

	public LocalDateTime getAppliedAt() {
		return appliedAt;
	}

	public void setAppliedAt(LocalDateTime appliedAt) {
		this.appliedAt = appliedAt;
	}

	public String getApplicantEmail() {
		return applicantEmail;
	}

	public void setApplicantEmail(String applicantEmail) {
		this.applicantEmail = applicantEmail;
	}

	public String getAadhaarNumber() {
		return aadhaarNumber;
	}

	public void setAadhaarNumber(String aadhaarNumber) {
		this.aadhaarNumber = aadhaarNumber;
	}

	public String getPanNumber() {
		return panNumber;
	}

	public void setPanNumber(String panNumber) {
		this.panNumber = panNumber;
	}
}
