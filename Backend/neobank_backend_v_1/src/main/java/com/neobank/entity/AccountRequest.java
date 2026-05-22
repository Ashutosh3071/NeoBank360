// src/main/java/com/neobank/entity/AccountRequest.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "account_requests",
    indexes = {
        @Index(name = "idx_ar_user",   columnList = "user_id"),
        @Index(name = "idx_ar_status", columnList = "status"),
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true, nullable = false, length = 20)
    private String requestId;  // REQ2026XXXXXXXX

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private Account.AccountType accountType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // New account if approved
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_account_id")
    private Account createdAccount;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "ifsc_code", length = 15)
    private String ifscCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }
}