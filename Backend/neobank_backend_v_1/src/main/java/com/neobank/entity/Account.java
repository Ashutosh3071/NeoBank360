// src/main/java/com/neobank/entity/Account.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "accounts",
    indexes = {
        @Index(name = "idx_acc_number",      columnList = "account_number",  unique = true),
        @Index(name = "idx_acc_user",        columnList = "user_id"),
        @Index(name = "idx_acc_application", columnList = "application_id"),
        @Index(name = "idx_acc_status",      columnList = "status"),
        @Index(name = "idx_acc_type",        columnList = "account_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    // ─────────────────────────────────────────────
    //  PRIMARY KEY
    // ─────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────────────
    //  ACCOUNT IDENTITY
    // ─────────────────────────────────────────────

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;           // e.g. NB20260000001ACC

    @Column(name = "account_number_display", length = 20)
    private String accountNumberDisplay;    // masked for UI: **** **** 0001

    // ─────────────────────────────────────────────
    //  LINKED ENTITIES
    // ─────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "application_id", length = 20)
    private String applicationId;          // Links to Application

    // ─────────────────────────────────────────────
    //  ACCOUNT TYPE & CATEGORY
    // ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType accountType;        // SAVINGS / CURRENT / SALARY

    @Enumerated(EnumType.STRING)
    @Column(name = "account_category", nullable = false, length = 20)
    private AccountCategory accountCategory = AccountCategory.INDIVIDUAL;

    // ─────────────────────────────────────────────
    //  BALANCE
    // ─────────────────────────────────────────────

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;  // balance - hold amount

    @Column(name = "hold_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal holdAmount = BigDecimal.ZERO;        // amount on hold

    @Column(name = "minimum_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal minimumBalance = BigDecimal.ZERO;    // required min balance

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    // ─────────────────────────────────────────────
    //  BRANCH DETAILS
    // ─────────────────────────────────────────────

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "branch_code", length = 10)
    private String branchCode;

    @Column(name = "ifsc_code", length = 15)
    private String ifscCode;               // e.g. NEOB0001234

    @Column(name = "micr_code", length = 10)
    private String micrCode;

    // ─────────────────────────────────────────────
    //  INTEREST
    // ─────────────────────────────────────────────

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;        // e.g. 4.00 for 4%

    @Column(name = "interest_credited_on")
    private LocalDate interestCreditedOn;

    // ─────────────────────────────────────────────
    //  LIMITS
    // ─────────────────────────────────────────────

    @Column(name = "daily_transfer_limit", precision = 15, scale = 2)
    private BigDecimal dailyTransferLimit = new BigDecimal("100000.00");

    @Column(name = "daily_withdrawal_limit", precision = 15, scale = 2)
    private BigDecimal dailyWithdrawalLimit = new BigDecimal("50000.00");

    @Column(name = "per_transaction_limit", precision = 15, scale = 2)
    private BigDecimal perTransactionLimit = new BigDecimal("50000.00");

    // ─────────────────────────────────────────────
    //  STATUS
    // ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "status_reason", length = 255)
    private String statusReason;            // reason if frozen/closed

    // ─────────────────────────────────────────────
    //  IMPORTANT DATES
    // ─────────────────────────────────────────────

    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;             // Date account was opened

    @Column(name = "closed_on")
    private LocalDate closedOn;             // Date if closed

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @Column(name = "last_statement_date")
    private LocalDate lastStatementDate;

    // ─────────────────────────────────────────────
    //  NOMINEE
    // ─────────────────────────────────────────────

    @Column(name = "nominee_name", length = 100)
    private String nomineeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "nominee_relation", length = 10)
    private NomineeRelation nomineeRelation;

    @Column(name = "nominee_age")
    private Integer nomineeAge;

    @Column(name = "nominee_mobile", length = 10)
    private String nomineeMobile;

    @Column(name = "nominee_address", length = 255)
    private String nomineeAddress;

    // ─────────────────────────────────────────────
    //  FEATURES FLAGS
    // ─────────────────────────────────────────────

    @Column(name = "net_banking_enabled", nullable = false)
    private Boolean netBankingEnabled = true;

    @Column(name = "mobile_banking_enabled", nullable = false)
    private Boolean mobileBankingEnabled = true;

    @Column(name = "upi_enabled", nullable = false)
    private Boolean upiEnabled = true;

    @Column(name = "debit_card_enabled", nullable = false)
    private Boolean debitCardEnabled = false;   // enabled after card issuance

    @Column(name = "cheque_book_enabled", nullable = false)
    private Boolean chequeBookEnabled = false;

    @Column(name = "sms_alerts_enabled", nullable = false)
    private Boolean smsAlertsEnabled = true;

    @Column(name = "email_alerts_enabled", nullable = false)
    private Boolean emailAlertsEnabled = true;

    // ─────────────────────────────────────────────
    //  AUDIT
    // ─────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;              // Admin username who created

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // ─────────────────────────────────────────────
    //  HELPER METHODS
    // ─────────────────────────────────────────────

    /** Recalculate available balance = balance - holdAmount */
    public void recalculateAvailableBalance() {
        this.availableBalance = this.balance.subtract(this.holdAmount);
    }

    /** Check if account has sufficient available balance */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.availableBalance.compareTo(amount) >= 0;
    }

    /** Check if account is operable */
    public boolean isOperable() {
        return this.status == AccountStatus.ACTIVE;
    }

    /** Mask account number for display */
    public String getMaskedAccountNumber() {
        if (this.accountNumber == null || this.accountNumber.length() < 4) return this.accountNumber;
        return "*".repeat(this.accountNumber.length() - 4) +
               this.accountNumber.substring(this.accountNumber.length() - 4);
    }

    // ─────────────────────────────────────────────
    //  ENUMS
    // ─────────────────────────────────────────────

    public enum AccountType {
        SAVINGS,    // Regular savings account
        CURRENT,    // Business current account
        SALARY      // Salary account
    }

    public enum AccountCategory {
        INDIVIDUAL,  // Single person
        JOINT,       // Joint account (future)
        MINOR,       // Minor account (future)
        CORPORATE    // Business account (future)
    }

    public enum AccountStatus {
        ACTIVE,      // Normal operation
        INACTIVE,    // No transactions for long time
        FROZEN,      // Temporarily frozen by admin/bank
        DORMANT,     // No activity for 2+ years
        CLOSED,      // Permanently closed
        SUSPENDED    // Suspended due to suspicious activity
    }

    public enum NomineeRelation {
        SPOUSE, FATHER, MOTHER, SON, DAUGHTER, SIBLING, OTHER
    }
}