// src/main/java/com/neobank/service/AuthService.java

package com.neobank.service;

import com.neobank.config.JwtUtil;
import com.neobank.dto.auth.*;
import com.neobank.entity.OtpVerification;
import com.neobank.entity.OtpVerification.OtpPurpose;
import com.neobank.entity.User;
import com.neobank.entity.User.*;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.OtpVerificationRepository;
import com.neobank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository            userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final CaptchaService            captchaService;
    private final MailService               mailService;
    private final PasswordEncoder           passwordEncoder;
    private final ApplicationService        applicationService;
//    private final AccountServise            accountServise;
    private final JwtUtil                   jwtUtil;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_MAX_ATTEMPTS   = 5;

    // ─────────────────────────────────────────────
    //  CHECK USERNAME AVAILABILITY (real-time)
    // ─────────────────────────────────────────────

    public UsernameCheckDTO checkUsername(String username) {
        if (username == null || username.trim().length() < 4) {
            return UsernameCheckDTO.builder()
                    .username(username)
                    .available(false)
                    .message("Username must be at least 4 characters.")
                    .build();
        }

        if (!username.matches("^[a-zA-Z0-9._]{4,30}$")) {
            return UsernameCheckDTO.builder()
                    .username(username)
                    .available(false)
                    .message("Only letters, numbers, dots and underscores allowed.")
                    .build();
        }

        boolean available = userRepository.isUsernameAvailable(username);
        return UsernameCheckDTO.builder()
                .username(username)
                .available(available)
                .message(available ? "Username is available." : "Username is already taken.")
                .build();
    }

    // ─────────────────────────────────────────────
    //  STEP 1: SEND REGISTRATION OTP
    // ─────────────────────────────────────────────

    @Transactional
    public void sendRegistrationOtp(String email, String captchaToken, String captchaAnswer) {

        // Validate captcha
        captchaService.validate(captchaToken, captchaAnswer);

        // Check email not already registered
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new NeoBankException(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS",
                "This email is already registered. Please login or use a different email."
            );
        }

        // Send OTP
        sendOtp(email, OtpPurpose.REGISTRATION);
    }

    // ─────────────────────────────────────────────
    //  STEP 2: REGISTER (verify OTP + create user)
    // ─────────────────────────────────────────────
    
    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO req) {

        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Passwords do not match.");
        }
        if (req.getPassword().length() < 6) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT", "Password must be at least 6 characters.");
        }
        if (!userRepository.isUsernameAvailable(req.getUsername())) {
            throw new NeoBankException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username is already taken.");
        }

        // Verify OTP first
        verifyOtp(req.getEmail(), req.getOtp(), OtpPurpose.REGISTRATION);

        Optional<User> existingUser = userRepository.findByEmail(req.getEmail().toLowerCase());

        User saved;

        if (existingUser.isPresent()) {
            // ── User was pre-created by admin on approval ──
            // Update credentials only — keep all profile data
            User user = existingUser.get();

            // Check if self-registered already (don't allow double registration)
            if ("SELF_REGISTRATION".equals(user.getCreatedBy())) {
                throw new NeoBankException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    "This email is already registered. Please login."
                );
            }

            // Update login credentials + username
            user.setUsername(req.getUsername().trim());
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            user.setIsLocked(false);
            user.setFailedLoginAttempts(0);
            user.setUpdatedBy("SELF_REGISTRATION");
            // Note: createdBy stays as admin name — audit trail preserved

            saved = userRepository.save(user);
            log.info("Admin-created user self-registered: {} ({})", saved.getUsername(), saved.getEmail());

        } else {
            // ── Brand new registration ──
            if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
                throw new NeoBankException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "This email is already registered.");
            }

            User newUser = User.builder()
                    .email(req.getEmail().toLowerCase().trim())
                    .username(req.getUsername().trim())
                    .passwordHash(passwordEncoder.encode(req.getPassword()))
                    .emailVerified(true)
                    .role(Role.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .failedLoginAttempts(0)
                    .isLocked(false)
                    .createdBy("SELF_REGISTRATION")
                    .build();
            
            if (userRepository.count() == 0) {
                newUser.setRole(Role.ADMIN);
            }

            saved = userRepository.save(newUser);
            log.info("New user registered: {} ({})", saved.getUsername(), saved.getEmail());
        }

        sendWelcomeEmail(saved);

        return RegisterResponseDTO.builder()
                .userId(saved.getId())
                .email(saved.getEmail())
                .username(saved.getUsername())
                .message("Registration successful! Welcome to NeoBank.")
                .build();
    }
    
//    @Transactional
//    public RegisterResponseDTO register(RegisterRequestDTO req) {
//
//        // Validate passwords
//        if (!req.getPassword().equals(req.getConfirmPassword())) {
//            throw new NeoBankException(
//                HttpStatus.BAD_REQUEST,
//                "PASSWORD_MISMATCH",
//                "Passwords do not match."
//            );
//        }
//
//        if (req.getPassword().length() < 6) {
//            throw new NeoBankException(
//                HttpStatus.BAD_REQUEST,
//                "PASSWORD_TOO_SHORT",
//                "Password must be at least 6 characters."
//            );
//        }
//
//        // Check email uniqueness
//        if (userRepository.existsByEmail(req.getEmail().toLowerCase())) {
//            throw new NeoBankException(
//                HttpStatus.CONFLICT,
//                "EMAIL_ALREADY_EXISTS",
//                "This email is already registered."
//            );
//        }
//
//        // Check username uniqueness
//        if (!userRepository.isUsernameAvailable(req.getUsername())) {
//            throw new NeoBankException(
//                HttpStatus.CONFLICT,
//                "USERNAME_TAKEN",
//                "Username is already taken. Please choose another."
//            );
//        }
//
//        // Verify OTP
//        verifyOtp(req.getEmail(), req.getOtp(), OtpPurpose.REGISTRATION);
//        
////        if (applicationService.isUsernameAvailable(req.getEmail())) {
////            throw new NeoBankException(
////                HttpStatus.CONFLICT,
////                "USERNAME_TAKEN",
////                "Username is already taken. Please choose another."
////            );
////        }
//
//        // Create user
//        User user = User.builder()
//                .email(req.getEmail().toLowerCase().trim())
//                .username(req.getUsername().trim())
//                .passwordHash(passwordEncoder.encode(req.getPassword()))
//                .emailVerified(true)
//                .role(Role.CUSTOMER)
//                .status(UserStatus.ACTIVE)
//                .failedLoginAttempts(0)
//                .isLocked(false)
//                .createdBy("SELF_REGISTRATION")
//                .build();
//
//        User saved = userRepository.save(user);
//
//        // Send welcome email
//        sendWelcomeEmail(saved);
//
//        log.info("User registered: {} ({})", saved.getUsername(), saved.getEmail());
//
//        return RegisterResponseDTO.builder()
//                .userId(saved.getId())
//                .email(saved.getEmail())
//                .username(saved.getUsername())
//                .message("Registration successful! Welcome to NeoBank.")
//                .build();
//    }

    // ─────────────────────────────────────────────
    //  LOGIN
    // ─────────────────────────────────────────────

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO req) {
    		
//        // Find user by email or username
//        User user = userRepository
//                .findByEmailOrUsername(
//                    req.getUsernameOrEmail().toLowerCase(),
//                    req.getUsernameOrEmail()
//                )
    	String input = req.getUsernameOrEmail().trim();
    	User user = userRepository.findByEmailOrUsername(
    	    input.toLowerCase(),   // email — emails are lowercased on save
    	    input                  // username — exact match
    	)
                .orElseThrow(() -> new NeoBankException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Invalid username/email or password."
                ));

        // Check locked
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new NeoBankException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_LOCKED",
                "Account is locked due to too many failed attempts. Please reset your password."
            );
        }

        // Check status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new NeoBankException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_INACTIVE",
                "Account is " + user.getStatus().name().toLowerCase() + ". Contact support."
            );
        }

        // Verify password
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            // Increment failed attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setIsLocked(true);
                user.setStatus(UserStatus.LOCKED);
            }
            userRepository.save(user);

            int remaining = Math.max(0, 5 - user.getFailedLoginAttempts());
            throw new NeoBankException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid password. " + remaining + " attempt(s) remaining."
            );
        }

        // Reset failed attempts on success
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getUsername());

        return LoginResponseDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
//                .accountNumber(user.getAccountNumber())
                .role(user.getRole())
                .token(jwtUtil.generateToken(user.getUsername(), user.getRole().name()))
                .build();
    }

    // ─────────────────────────────────────────────
    //  FORGOT USERNAME
    // ─────────────────────────────────────────────

    @Transactional
    public void forgotUsername(ForgotUsernameRequestDTO req) {

        // Validate captcha
        captchaService.validate(req.getCaptchaToken(), req.getCaptchaAnswer());

        // Find user
        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new NeoBankException(
                    HttpStatus.NOT_FOUND,
                    "EMAIL_NOT_FOUND",
                    "No account found with this email."
                ));

        // Send OTP
        sendOtp(req.getEmail(), OtpPurpose.USERNAME_RECOVERY);
    }

    @Transactional
    public void verifyForgotUsernameOtp(OtpRequestDTO req) {

        // Verify OTP
        verifyOtp(req.getEmail(), req.getOtp(), OtpPurpose.USERNAME_RECOVERY);

        // Send username via email
        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new NeoBankException(
                    HttpStatus.NOT_FOUND,
                    "EMAIL_NOT_FOUND",
                    "No account found with this email."
                ));

        mailService.sendMail(
            user.getEmail(),
            "NeoBank — Your Username",
            """
            Dear %s,

            Your NeoBank username is:

            Username: %s

            You can use this to login at NeoBank.

            If you did not request this, please contact support immediately.

            Regards,
            NeoBank Team
            """.formatted(
                user.getFullName() != null ? user.getFullName() : "Customer",
                user.getUsername()
            )
        );
    }

    // ─────────────────────────────────────────────
    //  FORGOT PASSWORD
    // ─────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO req) {

        // Validate captcha
        captchaService.validate(req.getCaptchaToken(), req.getCaptchaAnswer());

        // Check email exists
        if (!userRepository.existsByEmail(req.getEmail().toLowerCase())) {
            throw new NeoBankException(
                HttpStatus.NOT_FOUND,
                "EMAIL_NOT_FOUND",
                "No account found with this email."
            );
        }

        // Send OTP
        sendOtp(req.getEmail(), OtpPurpose.PASSWORD_RESET);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO req) {

        // Validate passwords match
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_MISMATCH",
                "Passwords do not match."
            );
        }

        if (req.getNewPassword().length() < 6) {
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST,
                "PASSWORD_TOO_SHORT",
                "Password must be at least 6 characters."
            );
        }

        // Verify OTP
        verifyOtp(req.getEmail(), req.getOtp(), OtpPurpose.PASSWORD_RESET);

        // Update password
        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new NeoBankException(
                    HttpStatus.NOT_FOUND,
                    "USER_NOT_FOUND",
                    "No account found with this email."
                ));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setIsLocked(false);
        user.setFailedLoginAttempts(0);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Notify user
        mailService.sendMail(
            user.getEmail(),
            "NeoBank — Password Changed Successfully",
            """
            Dear %s,

            Your NeoBank password has been changed successfully.

            If you did not make this change, please contact support immediately.

            Regards,
            NeoBank Team
            """.formatted(user.getFullName() != null ? user.getFullName() : "Customer")
        );

        log.info("Password reset for: {}", user.getEmail());
    }

    // ─────────────────────────────────────────────
    //  RESEND OTP
    // ─────────────────────────────────────────────

    @Transactional
    public void resendOtp(String email, String purposeStr) {
        OtpPurpose purpose = OtpPurpose.valueOf(purposeStr);
        otpVerificationRepository.deleteAllByEmailAndPurpose(email, purpose);
        sendOtp(email, purpose);
    }

    // ─────────────────────────────────────────────
    //  PRIVATE — OTP HELPERS
    // ─────────────────────────────────────────────

    private void sendOtp(String email, OtpPurpose purpose) {

        otpVerificationRepository.deleteAllByEmailAndPurpose(email, purpose);

        String code = String.format("%06d", new Random().nextInt(999999));

        otpVerificationRepository.save(OtpVerification.builder()
                .email(email.toLowerCase())
				.otpCode(
						code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .attemptCount(0)
                .maxAttempts(OTP_MAX_ATTEMPTS)
                .build());

        String subject = switch (purpose) {
            case REGISTRATION      -> "NeoBank — OTP to Complete Registration";
            case PASSWORD_RESET    -> "NeoBank — OTP to Reset Password";
            case USERNAME_RECOVERY -> "NeoBank — OTP to Recover Username";
            default                -> "NeoBank — OTP Verification";
        };

        String body = """
            Dear Customer,

            Your NeoBank OTP is:

            OTP: %s

            Valid for %d minutes. Do not share with anyone.

            Regards,
            NeoBank Team
            """.formatted(code, OTP_EXPIRY_MINUTES);

        mailService.sendMail(email, subject, body);
        log.info("OTP sent to {} for purpose {}", email, purpose);
    }

    private void verifyOtp(String email, String code, OtpPurpose purpose) {

        OtpVerification otp = otpVerificationRepository
                .findLatestActiveOtp(email.toLowerCase(), purpose,LocalDateTime.now())
                .orElseThrow(() -> new NeoBankException(
                    HttpStatus.BAD_REQUEST,
                    "OTP_NOT_FOUND",
                    "No OTP found. Please request a new one."
                ));

        if (otp.isExpired()) throw new NeoBankException(
            HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "OTP expired. Please request a new one."
        );

        if (otp.isExhausted()) throw new NeoBankException(
            HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS",
            "Max attempts reached. Please request a new OTP."
        );

        if (!otp.getOtpCode().equals(code)) {
            otp.incrementAttempt();
            otpVerificationRepository.save(otp);
            int remaining = otp.getMaxAttempts() - otp.getAttemptCount();
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST, "OTP_INVALID",
                "Incorrect OTP. " + remaining + " attempt(s) remaining."
            );
        }

        otp.markVerified();
        otpVerificationRepository.save(otp);
        otpVerificationRepository.deleteAllByEmailAndPurpose(email.toLowerCase(), purpose);
    }

    private void sendWelcomeEmail(User user) {
        mailService.sendMail(
            user.getEmail(),
            "Welcome to NeoBank! 🎉",
            """
            Dear %s,

            Welcome to NeoBank! Your account has been created successfully.

            Username : %s
            Email    : %s

            You can now login and manage your banking needs.

            Regards,
            NeoBank Team
            """.formatted(user.getUsername(), user.getUsername(), user.getEmail())
        );
    }
    
    @Transactional
    public void changePassword(ChangePasswordRequestDTO req, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NeoBankException(
                    HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "Current password is incorrect.");
        }

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "Passwords do not match.");
        }

        if (req.getNewPassword().length() < 6) {
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT", "Minimum 6 characters.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        mailService.sendMail(user.getEmail(),
            "NeoBank — Password Changed",
            "Dear " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) +
            ",\n\nYour password was changed successfully.\n\nIf not you, contact support immediately.\n\nNeoBank Team");
    }
}