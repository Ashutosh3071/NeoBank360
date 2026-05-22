// src/main/java/com/neobank/entity/OtpVerification.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "otp_verifications",
    indexes = {
        @Index(name = "idx_otp_email",       columnList = "email"),
        @Index(name = "idx_otp_purpose",     columnList = "purpose"),
        @Index(name = "idx_otp_expires",     columnList = "expires_at"),
        @Index(name = "idx_otp_invalidated", columnList = "invalidated")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    // ─────────────────────────────────────────────
    //  PRIMARY KEY
    // ─────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─────────────────────────────────────────────
    //  TARGET EMAIL
    // ─────────────────────────────────────────────

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    // ─────────────────────────────────────────────
    //  OTP DETAILS
    // ─────────────────────────────────────────────

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // ─────────────────────────────────────────────
    //  INVALIDATED FLAG
    //  Set to true when:
    //   - A new OTP is generated (old ones become invalid)
    //   - OTP is successfully verified
    //   - Max attempts are exceeded
    // ─────────────────────────────────────────────

    @Column(name = "invalidated", nullable = false)
    @Builder.Default
    private Boolean invalidated = false;

    // ─────────────────────────────────────────────
    //  ATTEMPT TRACKING
    // ─────────────────────────────────────────────

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    // ─────────────────────────────────────────────
    //  PURPOSE
    // ─────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    @Builder.Default
    private OtpPurpose purpose = OtpPurpose.APPLICATION_SUBMIT;

    // ─────────────────────────────────────────────
    //  LINK TO APPLICATION (optional)
    // ─────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id_fk", referencedColumnName = "id")
    private Application application;

    // ─────────────────────────────────────────────
    //  AUDIT
    // ─────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ─────────────────────────────────────────────
    //  HELPER METHODS
    // ─────────────────────────────────────────────

    /** OTP is expired if current time is past expiresAt */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /** OTP is exhausted if max attempts reached */
    public boolean isExhausted() {
        return this.attemptCount >= this.maxAttempts;
    }

    /**
     * OTP is fully valid only when:
     *  - Not expired
     *  - Not exhausted (too many wrong attempts)
     *  - Not invalidated (a newer OTP was sent)
     *  - Not already verified
     */
    public boolean isValid() {
        return !isExpired()
            && !isExhausted()
            && !Boolean.TRUE.equals(this.invalidated)
            && !Boolean.TRUE.equals(this.verified);
    }

    /** Increment wrong attempt count */
    public void incrementAttempt() {
        this.attemptCount++;
        // Auto-invalidate when max attempts exceeded
        if (this.attemptCount >= this.maxAttempts) {
            this.invalidated = true;
        }
    }

    /** Mark as successfully verified — also invalidate to prevent reuse */
    public void markVerified() {
        this.verified    = true;
        this.verifiedAt  = LocalDateTime.now();
        this.invalidated = true;  // prevent reuse
    }

    /** Manually invalidate this OTP (e.g. when a newer one is sent) */
    public void invalidate() {
        this.invalidated = true;
    }

    // ─────────────────────────────────────────────
    //  ENUMS
    // ─────────────────────────────────────────────

    public enum OtpPurpose {
        APPLICATION_SUBMIT,
        STATUS_CHECK,
        REGISTRATION,
        PASSWORD_RESET,
        USERNAME_RECOVERY,
        EMAIL_CHANGE,
        UPI_PIN_SETUP
    }
}