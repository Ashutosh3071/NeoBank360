// src/main/java/com/neobank/entity/UpiId.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "upi_ids",
    indexes = {
        @Index(name = "idx_upi_vpa",     columnList = "vpa",        unique = true),
        @Index(name = "idx_upi_account", columnList = "account_id"),
        @Index(name = "idx_upi_user",    columnList = "user_id"),
        @Index(name = "idx_upi_primary", columnList = "is_primary"),
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpiId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. john@neobank
    @Column(name = "vpa", unique = true, nullable = false, length = 100)
    private String vpa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Hashed 4 or 6 digit PIN
    @Column(name = "upi_pin_hash", length = 100)
    private String upiPinHash;

    @Column(name = "pin_set")
    @Builder.Default
    private Boolean pinSet = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UpiStatus status = UpiStatus.ACTIVE;

    // Only one UPI ID per account can be primary
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "daily_limit")
    @Builder.Default
    private Long dailyLimit = 100000L;

    @Column(name = "per_txn_limit")
    @Builder.Default
    private Long perTxnLimit = 100000L;

    @Column(name = "failed_pin_attempts")
    @Builder.Default
    private Integer failedPinAttempts = 0;

    @Column(name = "pin_locked_until")
    private LocalDateTime pinLockedUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // ── Enums ──
    public enum UpiStatus {
        ACTIVE, BLOCKED, DELETED
    }

    // ── Helpers ──
    public boolean isPinLocked() {
        return pinLockedUntil != null
            && LocalDateTime.now().isBefore(pinLockedUntil);
    }

    public void incrementFailedPin() {
        this.failedPinAttempts++;
        if (this.failedPinAttempts >= 3) {
            this.pinLockedUntil = LocalDateTime.now().plusHours(24);
        }
    }

    public void resetPinAttempts() {
        this.failedPinAttempts = 0;
        this.pinLockedUntil    = null;
    }
}