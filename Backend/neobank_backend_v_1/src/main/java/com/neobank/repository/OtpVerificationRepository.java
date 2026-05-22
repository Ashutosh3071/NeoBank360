// src/main/java/com/neobank/repository/OtpVerificationRepository.java

package com.neobank.repository;

import com.neobank.entity.OtpVerification;
import com.neobank.entity.OtpVerification.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    // ─────────────────────────────────────────────
    //  RATE LIMITING
    //  Count OTPs sent to this email+purpose in the last hour
    //  Used in OtpService.sendOtp() step 1
    // ─────────────────────────────────────────────

    @Query("""
        SELECT COUNT(o) FROM OtpVerification o
        WHERE o.email = :email
        AND o.purpose = :purpose
        AND o.createdAt >= :since
    """)
    long countRecentOtps(
            @Param("email")   String email,
            @Param("purpose") OtpPurpose purpose,
            @Param("since")   LocalDateTime since
    );

    // ─────────────────────────────────────────────
    //  COOLDOWN CHECK
    //  Find the most recently created OTP (any status)
    //  Used in OtpService.sendOtp() step 2
    // ─────────────────────────────────────────────

    @Query("""
        SELECT o FROM OtpVerification o
        WHERE o.email = :email
        AND o.purpose = :purpose
        ORDER BY o.createdAt DESC
        LIMIT 1
    """)
    Optional<OtpVerification> findLatestOtp(
            @Param("email")   String email,
            @Param("purpose") OtpPurpose purpose
    );

    // ─────────────────────────────────────────────
    //  INVALIDATE OLD OTPs
    //  Marks all unverified OTPs as invalidated before issuing a new one.
    //  Returns count of rows affected — used in OtpService.sendOtp() step 3.
    //
    //  NOTE: invalidated=true (not expiresAt manipulation) is the correct
    //  signal. OtpVerification.isValid() checks the invalidated flag,
    //  so setting expiresAt=now would be semantically misleading.
    // ─────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE OtpVerification o
        SET o.invalidated = true
        WHERE o.email = :email
        AND o.purpose = :purpose
        AND o.verified = false
        AND o.invalidated = false
    """)
    int invalidateAllActive(
            @Param("email")   String email,
            @Param("purpose") OtpPurpose purpose,
            @Param("now")     LocalDateTime now    // kept for API consistency; not used in query
    );

    // ─────────────────────────────────────────────
    //  VERIFY LOOKUP
    //  Find latest OTP that is unverified, not invalidated, and not expired.
    //  Used in OtpService.verifyOtp() step 1.
    // ─────────────────────────────────────────────

    @Query("""
        SELECT o FROM OtpVerification o
        WHERE o.email = :email
        AND o.purpose = :purpose
        AND o.verified = false
        AND o.invalidated = false
        AND o.expiresAt > :now
        ORDER BY o.createdAt DESC
        LIMIT 1
    """)
    Optional<OtpVerification> findLatestActiveOtp(
            @Param("email")   String email,
            @Param("purpose") OtpPurpose purpose,
            @Param("now")     LocalDateTime now
    );

    // ─────────────────────────────────────────────
    //  POST-VERIFICATION CLEANUP  (optional utility)
    //  Wipe all OTP records for this email+purpose after a successful flow.
    //  Call this from higher-level service code, not from OtpService itself.
    // ─────────────────────────────────────────────

    void deleteAllByEmailAndPurpose(String email, OtpPurpose purpose);
    
    // Get latest unverified, unexpired OTP for this email + purpose
    @Query("""
        SELECT o FROM OtpVerification o
        WHERE o.email = :email
        AND o.purpose = :purpose
        AND o.verified = false
        ORDER BY o.createdAt DESC
        LIMIT 1
    """)
    Optional<OtpVerification> findLatestActiveOtp(String email, OtpPurpose purpose);
}