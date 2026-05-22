// src/main/java/com/neobank/service/OtpService.java

package com.neobank.service;

import com.neobank.entity.OtpVerification;
import com.neobank.entity.OtpVerification.OtpPurpose;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepo;
    private final MailService               mailService;

    private static final int OTP_EXPIRY_MINUTES  = 10;
    private static final int MAX_ATTEMPTS        = 3;
    private static final int RATE_LIMIT_PER_HOUR = 5;
    private static final int COOLDOWN_SECONDS    = 60;

    // ─────────────────────────────────────────────
    //  SEND OTP
    //  Steps:
    //   1. Rate limit  — max 5 per hour
    //   2. Cooldown    — min 60s between sends
    //   3. Invalidate  — all previous active OTPs
    //   4. Generate    — new 6-digit code
    //   5. Save + send email
    // ─────────────────────────────────────────────

    @Transactional
    public void sendOtp(String email, OtpPurpose purpose,
                        String subject, String bodyTemplate) {

        // 1. Rate limit
        long recentCount = otpRepo.countRecentOtps(
                email, purpose,
                LocalDateTime.now().minusHours(1)
        );
        if (recentCount >= RATE_LIMIT_PER_HOUR) {
            throw new NeoBankException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "OTP_RATE_LIMITED",
                    "Too many OTP requests. Please wait before trying again."
            );
        }

        // 2. Cooldown
        otpRepo.findLatestOtp(email, purpose).ifPresent(last -> {
            long secondsSince = java.time.Duration
                    .between(last.getCreatedAt(), LocalDateTime.now())
                    .getSeconds();
            if (secondsSince < COOLDOWN_SECONDS) {
                long waitMore = COOLDOWN_SECONDS - secondsSince;
                throw new NeoBankException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "OTP_COOLDOWN",
                        "Please wait " + waitMore
                        + " second(s) before requesting a new OTP."
                );
            }
        });

        // 3. Invalidate all previous active OTPs for this email+purpose
        int invalidated = otpRepo.invalidateAllActive(
                email, purpose, LocalDateTime.now()
        );
        if (invalidated > 0) {
            log.info("Invalidated {} old OTP(s) for {} [{}]",
                    invalidated, email, purpose);
        }

        // 4. Generate code
        String code = String.format(
                "%06d", new Random().nextInt(900000) + 100000
        );

        // 5. Save
        OtpVerification otp = OtpVerification.builder()
                .email(email)
                .otpCode(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .invalidated(false)
                .attemptCount(0)
                .maxAttempts(MAX_ATTEMPTS)
                .build();

        otpRepo.save(otp);
        log.info("OTP generated for {} [{}]", email, purpose);

        // 6. Send email — replace {OTP} placeholder
        String body = bodyTemplate.replace("{OTP}", code);
        mailService.sendMail(email, subject, body);
    }

    // ─────────────────────────────────────────────
    //  VERIFY OTP
    //  Steps:
    //   1. Find latest active (not expired, not used, not invalidated)
    //   2. Check attempt count
    //   3. Match code
    //   4. Mark verified
    // ─────────────────────────────────────────────

    @Transactional
    public void verifyOtp(String email, OtpPurpose purpose, String submittedCode) {

        // 1. Find latest valid OTP
        OtpVerification otp = otpRepo
                .findLatestActiveOtp(email, purpose, LocalDateTime.now())
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.BAD_REQUEST,
                        "OTP_NOT_FOUND",
                        "No valid OTP found. Please request a new OTP."
                ));

        // 2. Check attempt exhaustion
        if (otp.isExhausted()) {
            throw new NeoBankException(
                    HttpStatus.BAD_REQUEST,
                    "OTP_MAX_ATTEMPTS",
                    "Maximum attempts reached. Please request a new OTP."
            );
        }

        // 3. Verify code
        if (!otp.getOtpCode().equals(submittedCode)) {
            otp.incrementAttempt();  // auto-invalidates at max attempts
            otpRepo.save(otp);

            int remaining = otp.getMaxAttempts() - otp.getAttemptCount();

            if (remaining <= 0) {
                throw new NeoBankException(
                        HttpStatus.BAD_REQUEST,
                        "OTP_MAX_ATTEMPTS",
                        "Too many wrong attempts. Please request a new OTP."
                );
            }

            throw new NeoBankException(
                    HttpStatus.BAD_REQUEST,
                    "OTP_INVALID",
                    "Incorrect OTP. " + remaining + " attempt(s) remaining."
            );
        }

        // 4. Mark verified (also sets invalidated=true to prevent reuse)
        otp.markVerified();
        otpRepo.save(otp);

        log.info("OTP verified for {} [{}]", email, purpose);
    }
}