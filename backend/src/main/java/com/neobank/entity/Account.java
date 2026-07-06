package com.neobank.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neobank.enums.AccountType;
import com.neobank.enums.AccountStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_number", nullable = false, unique = true, length = 30)
	private String accountNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false)
	private AccountType accountType;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal balance;

	/* ✅ NEW: account approval status */
	@Enumerated(EnumType.STRING)
	@Column(name = "account_status", nullable = false)
	private AccountStatus accountStatus;

	/* ✅ NEW: whether account can be used */
	@Column(name = "is_active", nullable = false)
	private Boolean isActive;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@PrePersist
	void prePersist() {

		if (balance == null) {
			balance = BigDecimal.ZERO;
		}

		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}

		// ✅ DEFAULT FOR NEW ACCOUNTS
		this.accountStatus = AccountStatus.PENDING_APPROVAL;
		this.isActive = false;
	}

	// -----------------------
	// Getters & Setters
	// -----------------------

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public void setAccountType(AccountType accountType) {
		this.accountType = accountType;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}