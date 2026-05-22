// src/main/java/com/neobank/entity/Transaction.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_txn_ref",        columnList = "reference_number", unique = true),
        @Index(name = "idx_txn_from_acc",   columnList = "from_account_id"),
        @Index(name = "idx_txn_to_acc",     columnList = "to_account_id"),
        @Index(name = "idx_txn_type",       columnList = "transaction_type"),
        @Index(name = "idx_txn_status",     columnList = "status"),
        @Index(name = "idx_txn_created_at", columnList = "created_at")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Transaction {

    // ─────────────────────────────────────────────
    //  PRIMARY KEY
    // ─────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", unique = true, nullable = false, length = 30)
    private String referenceNumber;        // e.g. TXN2026XXXXXXXXXX

    // ─────────────────────────────────────────────
    //  ACCOUNTS INVOLVED
    // ─────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;           // null for DEPOSIT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;             // null for WITHDRAWAL

    @Column(name = "from_account_number", length = 20)
    private String fromAccountNumber;      // denormalized for quick display

    @Column(name = "to_account_number", length = 20)
    private String toAccountNumber;

    // ─────────────────────────────────────────────
    //  AMOUNT & CURRENCY
    // ─────────────────────────────────────────────

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "charges", precision = 10, scale = 2)
    private BigDecimal charges = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;          // amount - charges

    // ─────────────────────────────────────────────
    //  BALANCE SNAPSHOTS
    // ─────────────────────────────────────────────

    @Column(name = "from_balance_before", precision = 15, scale = 2)
    private BigDecimal fromBalanceBefore;

    @Column(name = "from_balance_after", precision = 15, scale = 2)
    private BigDecimal fromBalanceAfter;

    @Column(name = "to_balance_before", precision = 15, scale = 2)
    private BigDecimal toBalanceBefore;

    @Column(name = "to_balance_after", precision = 15, scale = 2)
    private BigDecimal toBalanceAfter;

    // ─────────────────────────────────────────────
    //  TRANSACTION TYPE & MODE
    // ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_mode", nullable = false, length = 20)
    private TransactionMode transactionMode;

    // ─────────────────────────────────────────────
    //  UPI SPECIFIC
    // ─────────────────────────────────────────────

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "upi_reference", length = 50)
    private String upiReference;

    // ─────────────────────────────────────────────
    //  NEFT / RTGS SPECIFIC
    // ─────────────────────────────────────────────

    @Column(name = "beneficiary_name", length = 100)
    private String beneficiaryName;

    @Column(name = "beneficiary_account_number", length = 20)
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_ifsc", length = 15)
    private String beneficiaryIfsc;

    @Column(name = "beneficiary_bank_name", length = 100)
    private String beneficiaryBankName;

    // ─────────────────────────────────────────────
    //  DESCRIPTION & REMARKS
    // ─────────────────────────────────────────────

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "remarks", length = 500)
    private String remarks;

    // ─────────────────────────────────────────────
    //  STATUS
    // ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    // ─────────────────────────────────────────────
    //  INITIATED BY
    // ─────────────────────────────────────────────

    @Column(name = "initiated_by_user_id")
    private Long initiatedByUserId;

    @Column(name = "initiated_by_username", length = 50)
    private String initiatedByUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by_role", length = 20)
    private InitiatedByRole initiatedByRole;  // CUSTOMER, ADMIN, SYSTEM

    // ─────────────────────────────────────────────
    //  AUDIT
    // ─────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // ─────────────────────────────────────────────
    //  ENUMS
    // ─────────────────────────────────────────────

    public enum TransactionType {
        DEPOSIT,        // Admin/System adds money
        WITHDRAWAL,     // Admin/System removes money
        TRANSFER,       // Account to Account (internal)
        UPI,            // UPI payment
        NEFT,           // NEFT transfer
        RTGS,           // RTGS transfer
        IMPS,           // IMPS transfer
        INTEREST_CREDIT // System credits interest
    }

    public enum TransactionMode {
        ONLINE,    // Internet/Mobile banking
        ADMIN,     // Admin initiated
        SYSTEM,    // Automated system (interest etc.)
        ATM,       // Future use
        BRANCH     // Future use
    }

    public enum TransactionStatus {
        PENDING,    // Initiated
        SUCCESS,    // Completed
        FAILED,     // Failed
        REVERSED    // Reversed/Refunded
    }

    public enum InitiatedByRole {
        CUSTOMER, ADMIN, SYSTEM
    }
}