// src/main/java/com/neobank/service/ApplicationService.java

package com.neobank.service;

import com.neobank.dto.application.ApplicationDetailDTO;
import com.neobank.dto.application.ApplicationListDTO;
import com.neobank.dto.application.ApplicationRequestDTO;
import com.neobank.dto.application.ApplicationResponseDTO;
import com.neobank.dto.application.ApplicationStatusResponseDTO;
import com.neobank.dto.application.ApproveApplicationRequestDTO;
import com.neobank.dto.application.ApproveApplicationResponseDTO;
import com.neobank.entity.Account;
import com.neobank.entity.Application;
import com.neobank.entity.Application.*;
import com.neobank.entity.OtpVerification;
import com.neobank.entity.OtpVerification.OtpPurpose;
import com.neobank.entity.User;
import com.neobank.entity.User.Role;
import com.neobank.entity.User.UserStatus;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.ApplicationRepository;
import com.neobank.repository.OtpVerificationRepository;
import com.neobank.repository.UserRepository;
import com.neobank.util.AccountNumberGenerator;
import com.neobank.util.ApplicationIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;



@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository     applicationRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final ApplicationIdGenerator    applicationIdGenerator;
    private final MailService               mailService;
 // Add these fields in ApplicationService
    private final UserRepository        userRepository;
    private final AccountRepository     accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final PasswordEncoder       passwordEncoder;

    // ─────────────────────────────────────────────
    //  OTP EXPIRY MINUTES
    // ─────────────────────────────────────────────

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_MAX_ATTEMPTS   = 5;

    // ─────────────────────────────────────────────
    //  1. SEND OTP
    // ─────────────────────────────────────────────

    @Transactional
    public void sendOtp(String email) {
        // Invalidate any previous OTPs for this email
        otpVerificationRepository.deleteAllByEmailAndPurpose(email, OtpPurpose.APPLICATION_SUBMIT);

        // Generate 6-digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // Save OTP entity
        OtpVerification otpEntity = OtpVerification.builder()
                .email(email)
                .otpCode(otpCode)
                .purpose(OtpPurpose.APPLICATION_SUBMIT)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .attemptCount(0)
                .maxAttempts(OTP_MAX_ATTEMPTS)
                .build();

        otpVerificationRepository.save(otpEntity);

        // Send email
        sendOtpEmail(email, otpCode);

        log.info("OTP sent to email: {}", email);
    }

    // ─────────────────────────────────────────────
    //  2. SUBMIT APPLICATION (verify OTP + save)
    // ─────────────────────────────────────────────

    @Transactional
    public ApplicationResponseDTO submitApplication(ApplicationRequestDTO req) throws IOException {

        // ── A: Verify OTP first ──
        verifyOtp(req.getEmailId(), req.getOtp());

        // ── B: Check if active application already exists ──
        List<ApplicationStatus> activeStatuses = List.of(
                ApplicationStatus.PENDING,
                ApplicationStatus.SUBMITTED,
                ApplicationStatus.APPROVED
        );

        boolean hasActive = applicationRepository.existsActiveApplication(
                req.getEmailId(),
                req.getAadhaarNumber(),
                req.getPanNumber(),
                activeStatuses
        );

        if (hasActive) {
            throw new NeoBankException(
                    HttpStatus.CONFLICT,
                    "ACTIVE_APPLICATION_EXISTS",
                    "An active application already exists for this Aadhaar / PAN / Email. " +
                    "You can apply again only after your current application is rejected."
            );
        }

        // ── C: Parse enums safely ──
        AccountType accountType = parseEnum(AccountType.class,   req.getAccountType(),   "accountType");
        Gender      gender      = parseEnum(Gender.class,         req.getGender(),        "gender");
        MaritalStatus marital   = req.getMaritalStatus() != null && !req.getMaritalStatus().isBlank()
                                    ? parseEnum(MaritalStatus.class, req.getMaritalStatus(), "maritalStatus")
                                    : null;
        NomineeRelation relation = parseEnum(NomineeRelation.class, req.getNomineeRelation(), "nomineeRelation");

        // ── D: Parse date ──
        LocalDate dob = LocalDate.parse(req.getDateOfBirth());

        // ── E: Build Application entity ──
        Application app = Application.builder()
                .applicationId(applicationIdGenerator.generate())

                // Customer
                .accountType(accountType)
                .fullName(req.getFullName().trim())
                .fatherName(req.getFatherName().trim())
                .motherName(req.getMotherName().trim())
                .dateOfBirth(dob)
                .gender(gender)
                .maritalStatus(marital)
                .nationality(req.getNationality())
                .occupation(req.getOccupation().trim())
                .annualIncome(req.getAnnualIncome())
                .phoneNumber(req.getPhoneNumber().trim())
                .emailId(req.getEmailId().trim().toLowerCase())
                .aadhaarNumber(req.getAadhaarNumber().trim())
                .panNumber(req.getPanNumber().trim().toUpperCase())

                // Current Address
                .currentAddressLine(req.getCurrentAddressLine().trim())
                .currentCity(req.getCurrentCity().trim())
                .currentState(req.getCurrentState().trim())
                .currentPincode(req.getCurrentPincode().trim())

                // Permanent Address
                .permanentAddressLine(req.getPermanentAddressLine().trim())
                .permanentCity(req.getPermanentCity().trim())
                .permanentState(req.getPermanentState().trim())
                .permanentPincode(req.getPermanentPincode().trim())

                // Nominee
                .nomineeName(req.getNomineeName().trim())
                .nomineeRelation(relation)
                .nomineeAge(Integer.parseInt(req.getNomineeAge()))
                .nomineeMobileNumber(req.getNomineeMobileNumber().trim())
                .nomineeAddress(req.getNomineeAddress().trim())

                // Documents — store as BLOB
                .aadhaarCardFile(req.getAadhaarCardFile().getBytes())
                .aadhaarCardFileName(req.getAadhaarCardFile().getOriginalFilename())
                .aadhaarCardFileType(req.getAadhaarCardFile().getContentType())

                .panCardFile(req.getPanCardFile().getBytes())
                .panCardFileName(req.getPanCardFile().getOriginalFilename())
                .panCardFileType(req.getPanCardFile().getContentType())

                .profilePhoto(req.getProfilePhoto().getBytes())
                .profilePhotoName(req.getProfilePhoto().getOriginalFilename())
                .profilePhotoType(req.getProfilePhoto().getContentType())

                .signatureImage(req.getSignatureImage().getBytes())
                .signatureImageName(req.getSignatureImage().getOriginalFilename())
                .signatureImageType(req.getSignatureImage().getContentType())

                .addressProofDocument(req.getAddressProofDocument().getBytes())
                .addressProofDocumentName(req.getAddressProofDocument().getOriginalFilename())
                .addressProofDocumentType(req.getAddressProofDocument().getContentType())

                // OTP mark
                .otpVerified(true)
                .status(ApplicationStatus.SUBMITTED)
                .build();

        // Optional documents
        if (req.getPassportFile() != null && !req.getPassportFile().isEmpty()) {
            app.setPassportFile(req.getPassportFile().getBytes());
            app.setPassportFileName(req.getPassportFile().getOriginalFilename());
            app.setPassportFileType(req.getPassportFile().getContentType());
        }
        if (req.getVoterIdFile() != null && !req.getVoterIdFile().isEmpty()) {
            app.setVoterIdFile(req.getVoterIdFile().getBytes());
            app.setVoterIdFileName(req.getVoterIdFile().getOriginalFilename());
            app.setVoterIdFileType(req.getVoterIdFile().getContentType());
        }

        // ── F: Save application ──
        Application saved = applicationRepository.save(app);

        // ── G: Cleanup OTPs ──
        otpVerificationRepository.deleteAllByEmailAndPurpose(
                req.getEmailId(), OtpPurpose.APPLICATION_SUBMIT
        );

        // ── H: Send confirmation email ──
        sendConfirmationEmail(saved);

        log.info("Application submitted: {} for email: {}", saved.getApplicationId(), saved.getEmailId());

        return buildResponse(saved);
    }

    // ─────────────────────────────────────────────
    //  PRIVATE — OTP VERIFICATION
    // ─────────────────────────────────────────────

    private void verifyOtp(String email, String otpCode) {

        OtpVerification otp = otpVerificationRepository
                .findLatestActiveOtp(email, OtpPurpose.APPLICATION_SUBMIT)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.BAD_REQUEST,
                        "OTP_NOT_FOUND",
                        "No OTP found for this email. Please request a new OTP."
                ));

        // Check expiry
        if (otp.isExpired()) {
            throw new NeoBankException(
                    HttpStatus.BAD_REQUEST,
                    "OTP_EXPIRED",
                    "OTP has expired. Please request a new OTP."
            );
        }

        // Check max attempts
        if (otp.isExhausted()) {
            throw new NeoBankException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "OTP_MAX_ATTEMPTS",
                    "Maximum OTP attempts reached. Please request a new OTP."
            );
        }

        // Check OTP code
        if (!otp.getOtpCode().equals(otpCode)) {
            otp.incrementAttempt();
            otpVerificationRepository.save(otp);

            int remaining = otp.getMaxAttempts() - otp.getAttemptCount();
            throw new NeoBankException(
                    HttpStatus.BAD_REQUEST,
                    "OTP_INVALID",
                    "Incorrect OTP. " + remaining + " attempt(s) remaining."
            );
        }

        // Mark verified
        otp.markVerified();
        otpVerificationRepository.save(otp);
    }

    // ─────────────────────────────────────────────
    //  PRIVATE — EMAIL HELPERS
    // ─────────────────────────────────────────────

    private void sendOtpEmail(String email, String otpCode) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("NeoBank — Your OTP for Account Application");
            msg.setText("""
                Dear Applicant,

                Your One-Time Password (OTP) for NeoBank account application is:

                OTP: %s

                This OTP is valid for %d minutes.
                Do not share this OTP with anyone.

                If you did not request this, please ignore this email.

                Regards,
                NeoBank Team
                """.formatted(otpCode, OTP_EXPIRY_MINUTES));
            mailService.send(msg);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw new NeoBankException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "EMAIL_SEND_FAILED",
                    "Failed to send OTP. Please try again."
            );
        }
    }

    private void sendConfirmationEmail(Application app) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(app.getEmailId());
            msg.setSubject("NeoBank — Application Submitted Successfully");
            msg.setText("""
                Dear %s,

                Your NeoBank account application has been successfully submitted.

                Application Details:
                ─────────────────────────────
                Application ID : %s
                Account Type   : %s
                Status         : %s
                Submitted On   : %s
                ─────────────────────────────

                Our team will verify your documents within 2–3 business days.
                You will receive an update on this email.

                Regards,
                NeoBank Team
                """.formatted(
                        app.getFullName(),
                        app.getApplicationId(),
                        app.getAccountType(),
                        app.getStatus(),
                        app.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
            ));
            mailService.send(msg);
        } catch (Exception e) {
            // Don't fail the whole request if confirmation email fails
            log.warn("Confirmation email failed for application: {}", app.getApplicationId(), e);
        }
    }

    // ─────────────────────────────────────────────
    //  PRIVATE — HELPERS
    // ─────────────────────────────────────────────

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase().trim());
        } catch (Exception e) {
            throw new NeoBankException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FIELD",
                    "Invalid value for field: " + fieldName + " → " + value
            );
        }
    }

    private ApplicationResponseDTO buildResponse(Application app) {
        // Mask Aadhaar: ••••••••1234
        String maskedAadhaar = "••••••••" + app.getAadhaarNumber().substring(8);

        return ApplicationResponseDTO.builder()
                .applicationId(app.getApplicationId())
                .fullName(app.getFullName())
                .accountType(app.getAccountType().name())
                .phoneNumber(app.getPhoneNumber())
                .emailId(app.getEmailId())
                .aadhaarNumber(maskedAadhaar)
                .panNumber(app.getPanNumber())
                .status(app.getStatus())
                .submittedOn(app.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
                .build();
    }
    
 // ── Send OTP for status check ──
    @Transactional
    public void sendStatusOtp(String query) {

        // Find application by email or applicationId
        Application app = findApplicationByQuery(query);

        // Invalidate previous status OTPs
        otpVerificationRepository.deleteAllByEmailAndPurpose(
            app.getEmailId(), OtpPurpose.STATUS_CHECK
        );

        String otpCode = String.format("%06d", new Random().nextInt(999999));

        otpVerificationRepository.save(OtpVerification.builder()
                .email(app.getEmailId())
                .otpCode(otpCode)
                .purpose(OtpPurpose.STATUS_CHECK)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .attemptCount(0)
                .maxAttempts(OTP_MAX_ATTEMPTS)
                .build());

        mailService.sendMail(
            app.getEmailId(),
            "NeoBank — OTP to View Application Status",
            """
            Dear %s,

            Your OTP to view application status is:

            OTP: %s

            Valid for %d minutes. Do not share with anyone.

            Regards,
            NeoBank Team
            """.formatted(app.getFullName(), otpCode, OTP_EXPIRY_MINUTES)
        );
    }

    // ── Verify OTP and return full details ──
    @Transactional
    public ApplicationStatusResponseDTO verifyStatusOtpAndGetDetails(String query, String otpCode) {

        Application app = findApplicationByQuery(query);

        // Verify OTP
        OtpVerification otp = otpVerificationRepository
                .findLatestActiveOtp(app.getEmailId(), OtpPurpose.STATUS_CHECK)
                .orElseThrow(() -> new NeoBankException(
                    HttpStatus.BAD_REQUEST, "OTP_NOT_FOUND",
                    "No OTP found. Please request a new OTP."
                ));

        if (otp.isExpired()) throw new NeoBankException(
            HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "OTP expired. Please request a new one."
        );

        if (otp.isExhausted()) throw new NeoBankException(
            HttpStatus.TOO_MANY_REQUESTS, "OTP_MAX_ATTEMPTS",
            "Max attempts reached. Please request a new OTP."
        );

        if (!otp.getOtpCode().equals(otpCode)) {
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
        otpVerificationRepository.deleteAllByEmailAndPurpose(
            app.getEmailId(), OtpPurpose.STATUS_CHECK
        );

        return buildStatusResponse(app);
    }

    // ── Find by email or applicationId ──
    private Application findApplicationByQuery(String query) {
        if (query.contains("@")) {
            return applicationRepository
                    .findByEmailIdOrderByCreatedAtDesc(query.toLowerCase())
                    .stream().findFirst()
                    .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                        "No application found for email: " + query
                    ));
        } else {
            return applicationRepository
                    .findByApplicationId(query.toUpperCase())
                    .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "APPLICATION_NOT_FOUND",
                        "No application found for ID: " + query
                    ));
        }
    }

    // ── Build full status response ──
    private ApplicationStatusResponseDTO buildStatusResponse(Application app) {
        return ApplicationStatusResponseDTO.builder()
                .applicationId(app.getApplicationId())
                .fullName(app.getFullName())
                .fatherName(app.getFatherName())
                .motherName(app.getMotherName())
                .dateOfBirth(app.getDateOfBirth().toString())
                .gender(app.getGender().name())
                .maritalStatus(app.getMaritalStatus() != null ? app.getMaritalStatus().name() : "")
                .nationality(app.getNationality())
                .occupation(app.getOccupation())
                .annualIncome(app.getAnnualIncome())
                .accountType(app.getAccountType().name())
                .phoneNumber(app.getPhoneNumber())
                .emailId(app.getEmailId())
                .aadhaarNumber("••••••••" + app.getAadhaarNumber().substring(8))
                .panNumber(app.getPanNumber())
                .currentAddressLine(app.getCurrentAddressLine())
                .currentCity(app.getCurrentCity())
                .currentState(app.getCurrentState())
                .currentPincode(app.getCurrentPincode())
                .permanentAddressLine(app.getPermanentAddressLine())
                .permanentCity(app.getPermanentCity())
                .permanentState(app.getPermanentState())
                .permanentPincode(app.getPermanentPincode())
                .nomineeName(app.getNomineeName())
                .nomineeRelation(app.getNomineeRelation().name())
                .nomineeAge(String.valueOf(app.getNomineeAge()))
                .nomineeMobileNumber(app.getNomineeMobileNumber())
                .nomineeAddress(app.getNomineeAddress())
                .status(app.getStatus().name())
                .submittedOn(app.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
                .build();
    }
    
 // ─────────────────────────────────────────────
//  GET ALL APPLICATIONS (no documents)
// ─────────────────────────────────────────────

public Page<ApplicationListDTO> getAllApplications(
        ApplicationStatus status,
        Pageable pageable
) {
    Page<Application> page = (status != null)
            ? applicationRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            : applicationRepository.findAllApplications(pageable);

    return page.map(this::buildListDTO);
}

// ─────────────────────────────────────────────
//  GET APPLICATION BY ID (with documents)
// ─────────────────────────────────────────────

public ApplicationDetailDTO getApplicationById(String applicationId) {
    Application app = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new NeoBankException(
                HttpStatus.NOT_FOUND,
                "APPLICATION_NOT_FOUND",
                "Application not found: " + applicationId
            ));
    return buildDetailDTO(app);
}

// ─────────────────────────────────────────────
//  APPROVE / REJECT APPLICATION
// ─────────────────────────────────────────────

@Transactional
public ApproveApplicationResponseDTO approveApplication(
        ApproveApplicationRequestDTO req,
        String adminUsername
) {
    // ── Find application ──
    Application app = applicationRepository.findByApplicationId(req.getApplicationId())
            .orElseThrow(() -> new NeoBankException(
                HttpStatus.NOT_FOUND,
                "APPLICATION_NOT_FOUND",
                "Application not found: " + req.getApplicationId()
            ));

    // ── Validate status ──
    if (app.getStatus() != ApplicationStatus.SUBMITTED) {
        throw new NeoBankException(
            HttpStatus.CONFLICT,
            "INVALID_STATUS",
            "Application is already " + app.getStatus().name() + ". Cannot process again."
        );
    }

    // ── Find admin user ──
    User adminUser = userRepository.findByUsername(adminUsername)
            .orElseThrow(() -> new NeoBankException(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                "Admin user not found."
            ));

    // ── REJECT flow ──
    if ("REJECT".equalsIgnoreCase(req.getAction())) {
        app.setStatus(ApplicationStatus.REJECTED);
        app.setApprovedBy(adminUser);
        app.setApprovedAt(LocalDateTime.now());
        app.setRejectionReason(req.getRejectionReason());
        applicationRepository.save(app);

        // Notify applicant
        mailService.sendMail(
            app.getEmailId(),
            "NeoBank — Application Update",
            """
            Dear %s,

            We regret to inform you that your application (%s) has been rejected.

            Reason: %s

            You may apply again after resolving the above issue.

            Regards,
            NeoBank Team
            """.formatted(app.getFullName(), app.getApplicationId(),
                req.getRejectionReason() != null ? req.getRejectionReason() : "N/A")
        );

        log.info("Application REJECTED: {} by {}", app.getApplicationId(), adminUsername);

        return ApproveApplicationResponseDTO.builder()
                .applicationId(app.getApplicationId())
                .status("REJECTED")
                .message("Application rejected successfully.")
                .build();
    }

    // ── APPROVE flow ──

    // Step A: Find or create User
    User user = findOrCreateUser(app, adminUsername);

    // Step B: Create Account
    Account account = createAccount(app, user, req, adminUsername);

    // Step C: Update Application
    app.setStatus(ApplicationStatus.APPROVED);
    app.setApprovedBy(adminUser);
    app.setApprovedAt(LocalDateTime.now());
    applicationRepository.save(app);

    // Step D: Update User with application details
    updateUserFromApplication(user, app, account, adminUsername);

    // Step E: Send approval email with account details
    sendApprovalEmail(app, user, account);

    log.info("Application APPROVED: {} → Account: {} for User: {}",
            app.getApplicationId(), account.getAccountNumber(), user.getEmail());

    return ApproveApplicationResponseDTO.builder()
            .applicationId(app.getApplicationId())
            .status("APPROVED")
            .userId(String.valueOf(user.getId()))
            .accountNumber(account.getAccountNumber())
            .fullName(user.getFullName())
            .emailId(user.getEmail())
            .message("Application approved. Account created: " + account.getAccountNumber())
            .build();
}

// ─────────────────────────────────────────────
//  PRIVATE — Find or Create User
// ─────────────────────────────────────────────

private User findOrCreateUser(Application app, String adminUsername) {

    Optional<User> existingUser = userRepository.findByEmail(app.getEmailId().toLowerCase());

    if (existingUser.isPresent()) {
        // User already registered — update their profile details
        User user = existingUser.get();
        log.info("User already exists for email: {}. Updating profile.", app.getEmailId());
        // Don't overwrite credentials — only fill profile fields
        fillUserProfile(user, app);
        user.setUpdatedBy(adminUsername);
        user.setApplicationId(app.getApplicationId());
        return userRepository.save(user);
    }

    // User doesn't exist — create new user
    // Auto-generate username from name + random digits
    String baseUsername = app.getFullName()
            .toLowerCase()
            .replaceAll("[^a-z0-9]", "")
            .substring(0, Math.min(10, app.getFullName().replaceAll("[^a-zA-Z0-9]", "").length()));
    String username = generateUniqueUsername(baseUsername);

    // Temp password = aadhaar last 4 + phone last 4
    String tempPassword = app.getAadhaarNumber().substring(8) +
                          app.getPhoneNumber().substring(6);

    User newUser = User.builder()
            .email(app.getEmailId().toLowerCase())
            .username(username)
            .passwordHash(passwordEncoder.encode(tempPassword))
            .phone(app.getPhoneNumber())
            .emailVerified(true)
            .role(Role.CUSTOMER)
            .status(UserStatus.ACTIVE)
            .failedLoginAttempts(0)
            .isLocked(false)
            .applicationId(app.getApplicationId())
            .createdBy(adminUsername)
            .build();

    fillUserProfile(newUser, app);

    // Copy profile photo
    if (app.getProfilePhoto() != null) {
        newUser.setProfilePhoto(app.getProfilePhoto());
        newUser.setProfilePhotoType(app.getProfilePhotoType());
    }

    User saved = userRepository.save(newUser);
    log.info("New user created by admin approval: {} ({})", saved.getUsername(), saved.getEmail());
    return saved;
}

private void fillUserProfile(User user, Application app) {
    user.setFullName(app.getFullName());
    user.setFatherName(app.getFatherName());
    user.setMotherName(app.getMotherName());
    user.setPhone(app.getPhoneNumber());
    user.setDateOfBirth(app.getDateOfBirth());
    user.setGender(parseUserGender(app.getGender()));
    user.setMaritalStatus(parseUserMaritalStatus(app.getMaritalStatus()));
    user.setNationality(app.getNationality());
    user.setOccupation(app.getOccupation());
    user.setAnnualIncome(app.getAnnualIncome());
    user.setAadhaarNumber(app.getAadhaarNumber());
    user.setPanNumber(app.getPanNumber());
    user.setCurrentAddressLine(app.getCurrentAddressLine());
    user.setCurrentCity(app.getCurrentCity());
    user.setCurrentState(app.getCurrentState());
    user.setCurrentPincode(app.getCurrentPincode());
    user.setPermanentAddressLine(app.getPermanentAddressLine());
    user.setPermanentCity(app.getPermanentCity());
    user.setPermanentState(app.getPermanentState());
    user.setPermanentPincode(app.getPermanentPincode());
}

private String generateUniqueUsername(String base) {
    String username = base;
    int attempt = 0;
    while (!userRepository.isUsernameAvailable(username)) {
        attempt++;
        username = base + String.format("%03d", attempt);
    }
    return username;
}

// ─────────────────────────────────────────────
//  PRIVATE — Create Account
// ─────────────────────────────────────────────

private Account createAccount(
        Application app,
        User user,
        ApproveApplicationRequestDTO req,
        String adminUsername
) {
    Account account = Account.builder()
            .accountNumber(accountNumberGenerator.generate())
            .user(user)
            .applicationId(app.getApplicationId())
            .accountType(parseAccountType(app.getAccountType()))
            .accountCategory(Account.AccountCategory.INDIVIDUAL)
            .balance(BigDecimal.ZERO)
            .availableBalance(BigDecimal.ZERO)
            .holdAmount(BigDecimal.ZERO)
            .minimumBalance(getMinimumBalance(app.getAccountType()))
            .currency("INR")
            .branchName(req.getBranchName() != null ? req.getBranchName() : "Main Branch")
            .branchCode(req.getBranchCode() != null ? req.getBranchCode() : "001")
            .ifscCode(req.getIfscCode() != null ? req.getIfscCode() : "NEOB0000001")
            .interestRate(getInterestRate(app.getAccountType()))
            .dailyTransferLimit(new BigDecimal("100000.00"))
            .dailyWithdrawalLimit(new BigDecimal("50000.00"))
            .perTransactionLimit(new BigDecimal("50000.00"))
            .status(Account.AccountStatus.ACTIVE)
            .openedOn(LocalDate.now())
            .nomineeName(app.getNomineeName())
            .nomineeRelation(parseNomineeRelation(app.getNomineeRelation()))
            .nomineeAge(app.getNomineeAge())
            .nomineeMobile(app.getNomineeMobileNumber())
            .nomineeAddress(app.getNomineeAddress())
            .netBankingEnabled(true)
            .mobileBankingEnabled(true)
            .upiEnabled(true)
            .debitCardEnabled(false)
            .chequeBookEnabled(false)
            .smsAlertsEnabled(true)
            .emailAlertsEnabled(true)
            .createdBy(adminUsername)
            .build();

    account.setAccountNumberDisplay(account.getMaskedAccountNumber());
    return accountRepository.save(account);
}

private void updateUserFromApplication(
        User user,
        Application app,
        Account account,
        String adminUsername
) {
    user.setApplicationId(app.getApplicationId());
    user.setUpdatedBy(adminUsername);
    userRepository.save(user);
}

// ─────────────────────────────────────────────
//  PRIVATE — Email on Approval
// ─────────────────────────────────────────────

private void sendApprovalEmail(Application app, User user, Account account) {

    boolean wasNewUser = user.getCreatedBy() != null &&
                         !user.getCreatedBy().equals("SELF_REGISTRATION");

    String passwordInfo = wasNewUser
        ? """
          Your temporary login credentials:
          Username : %s
          Password : %s (last 4 of Aadhaar + last 4 of mobile)

          Please change your password after first login.
          """.formatted(
              user.getUsername(),
              app.getAadhaarNumber().substring(8) + app.getPhoneNumber().substring(6)
          )
        : "Username : " + user.getUsername() + "\n          Use your registered password to login.";

    mailService.sendMail(
        app.getEmailId(),
        "🎉 NeoBank — Your Account is Approved!",
        """
        Dear %s,

        Congratulations! Your NeoBank account application has been approved.

        ═══════════════════════════════════════
        ACCOUNT DETAILS
        ═══════════════════════════════════════
        Account Number : %s
        Account Type   : %s
        IFSC Code      : %s
        Branch         : %s
        Status         : ACTIVE
        Opened On      : %s
        ═══════════════════════════════════════

        LOGIN CREDENTIALS
        ═══════════════════════════════════════
        %s
        ═══════════════════════════════════════

        You can now:
        ✓ Login to NeoBank online banking
        ✓ Transfer funds via UPI / NEFT / IMPS
        ✓ View your account statements
        ✓ Manage your profile

        For support: support@neobank.in

        Regards,
        NeoBank Team
        """.formatted(
            app.getFullName(),
            account.getAccountNumber(),
            account.getAccountType().name(),
            account.getIfscCode(),
            account.getBranchName(),
            LocalDate.now().toString(),
            passwordInfo
        )
    );
}

// ─────────────────────────────────────────────
//  PRIVATE — Build List DTO (no docs)
// ─────────────────────────────────────────────

private ApplicationListDTO buildListDTO(Application app) {
    return ApplicationListDTO.builder()
            .applicationId(app.getApplicationId())
            .fullName(app.getFullName())
            .emailId(app.getEmailId())
            .phoneNumber(app.getPhoneNumber())
            .accountType(app.getAccountType().name())
            .gender(app.getGender().name())
            .occupation(app.getOccupation())
            .currentCity(app.getCurrentCity())
            .currentState(app.getCurrentState())
            .status(app.getStatus())
            .submittedOn(app.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
            .aadhaarNumber("••••••••" + app.getAadhaarNumber().substring(8))
            .panNumber(app.getPanNumber())
            .build();
}

// ─────────────────────────────────────────────
//  PRIVATE — Build Detail DTO (with docs)
// ─────────────────────────────────────────────

private ApplicationDetailDTO buildDetailDTO(Application app) {
    ApplicationDetailDTO dto = ApplicationDetailDTO.builder()
            .applicationId(app.getApplicationId())
            .status(app.getStatus())
            .submittedOn(app.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
            .approvedBy(app.getApprovedBy() != null ? app.getApprovedBy().getUsername() : null)
            .approvedAt(app.getApprovedAt() != null
                    ? app.getApprovedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                    : null)
            .rejectionReason(app.getRejectionReason())
            .accountType(app.getAccountType().name())
            .fullName(app.getFullName())
            .fatherName(app.getFatherName())
            .motherName(app.getMotherName())
            .dateOfBirth(app.getDateOfBirth().toString())
            .gender(app.getGender().name())
            .maritalStatus(app.getMaritalStatus() != null ? app.getMaritalStatus().name() : "")
            .nationality(app.getNationality())
            .occupation(app.getOccupation())
            .annualIncome(app.getAnnualIncome())
            .phoneNumber(app.getPhoneNumber())
            .emailId(app.getEmailId())
            .aadhaarNumber("••••••••" + app.getAadhaarNumber().substring(8))
            .panNumber(app.getPanNumber())
            .currentAddressLine(app.getCurrentAddressLine())
            .currentCity(app.getCurrentCity())
            .currentState(app.getCurrentState())
            .currentPincode(app.getCurrentPincode())
            .permanentAddressLine(app.getPermanentAddressLine())
            .permanentCity(app.getPermanentCity())
            .permanentState(app.getPermanentState())
            .permanentPincode(app.getPermanentPincode())
            .nomineeName(app.getNomineeName())
            .nomineeRelation(app.getNomineeRelation().name())
            .nomineeAge(String.valueOf(app.getNomineeAge()))
            .nomineeMobileNumber(app.getNomineeMobileNumber())
            .nomineeAddress(app.getNomineeAddress())
            .build();

    // Encode documents to base64
    if (app.getAadhaarCardFile() != null) {
        dto.setAadhaarCardFileType(app.getAadhaarCardFileType());
        dto.setAadhaarCardFileBase64(Base64.getEncoder().encodeToString(app.getAadhaarCardFile()));
    }
    if (app.getPanCardFile() != null) {
        dto.setPanCardFileType(app.getPanCardFileType());
        dto.setPanCardFileBase64(Base64.getEncoder().encodeToString(app.getPanCardFile()));
    }
    if (app.getProfilePhoto() != null) {
        dto.setProfilePhotoType(app.getProfilePhotoType());
        dto.setProfilePhotoBase64(Base64.getEncoder().encodeToString(app.getProfilePhoto()));
    }
    if (app.getSignatureImage() != null) {
        dto.setSignatureImageType(app.getSignatureImageType());
        dto.setSignatureImageBase64(Base64.getEncoder().encodeToString(app.getSignatureImage()));
    }
    if (app.getAddressProofDocument() != null) {
        dto.setAddressProofDocumentType(app.getAddressProofDocumentType());
        dto.setAddressProofDocumentBase64(Base64.getEncoder().encodeToString(app.getAddressProofDocument()));
    }
    if (app.getPassportFile() != null) {
        dto.setPassportFileType(app.getPassportFileType());
        dto.setPassportFileBase64(Base64.getEncoder().encodeToString(app.getPassportFile()));
    }
    if (app.getVoterIdFile() != null) {
        dto.setVoterIdFileType(app.getVoterIdFileType());
        dto.setVoterIdFileBase64(Base64.getEncoder().encodeToString(app.getVoterIdFile()));
    }

    return dto;
}

// ─────────────────────────────────────────────
//  PRIVATE — Enum Parsers
// ─────────────────────────────────────────────

private Account.AccountType parseAccountType(Application.AccountType type) {
    return switch (type) {
        case SAVINGS -> Account.AccountType.SAVINGS;
        case CURRENT -> Account.AccountType.CURRENT;
        case SALARY  -> Account.AccountType.SALARY;
    };
}

private Account.NomineeRelation parseNomineeRelation(Application.NomineeRelation r) {
    return switch (r) {
        case SPOUSE   -> Account.NomineeRelation.SPOUSE;
        case FATHER   -> Account.NomineeRelation.FATHER;
        case MOTHER   -> Account.NomineeRelation.MOTHER;
        case SON      -> Account.NomineeRelation.SON;
        case DAUGHTER -> Account.NomineeRelation.DAUGHTER;
        case SIBLING  -> Account.NomineeRelation.SIBLING;
        case OTHER    -> Account.NomineeRelation.OTHER;
    };
}

private User.Gender parseUserGender(Application.Gender g) {
    return switch (g) {
        case MALE   -> User.Gender.MALE;
        case FEMALE -> User.Gender.FEMALE;
        case OTHER  -> User.Gender.OTHER;
    };
}

private User.MaritalStatus parseUserMaritalStatus(Application.MaritalStatus m) {
    if (m == null) return null;
    return switch (m) {
        case SINGLE   -> User.MaritalStatus.SINGLE;
        case MARRIED  -> User.MaritalStatus.MARRIED;
        case DIVORCED -> User.MaritalStatus.DIVORCED;
        case WIDOWED  -> User.MaritalStatus.WIDOWED;
    };
}

private BigDecimal getMinimumBalance(Application.AccountType type) {
    return switch (type) {
        case SAVINGS -> new BigDecimal("1000.00");
        case CURRENT -> new BigDecimal("10000.00");
        case SALARY  -> BigDecimal.ZERO;
    };
}

private BigDecimal getInterestRate(Application.AccountType type) {
    return switch (type) {
        case SAVINGS -> new BigDecimal("4.00");
        case CURRENT -> BigDecimal.ZERO;
        case SALARY  -> new BigDecimal("3.50");
    };
}

// Add to ApplicationService.java

@Transactional
public ApplicationResponseDTO submitApplicationAuthenticated(
        ApplicationRequestDTO req, String username) throws IOException {

    // ── Check active application ──
    List<ApplicationStatus> activeStatuses = List.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.APPROVED
    );

    boolean hasActive = applicationRepository.existsActiveApplication(
            req.getEmailId(),
            req.getAadhaarNumber(),
            req.getPanNumber(),
            activeStatuses
    );

    if (hasActive) {
        throw new NeoBankException(HttpStatus.CONFLICT,
                "ACTIVE_APPLICATION_EXISTS",
                "An active application already exists for this Aadhaar / PAN / Email.");
    }

    // ── Parse enums ──
    AccountType     accountType = parseEnum(AccountType.class,     req.getAccountType(),    "accountType");
    Gender          gender      = parseEnum(Gender.class,           req.getGender(),         "gender");
    MaritalStatus   marital     = req.getMaritalStatus() != null && !req.getMaritalStatus().isBlank()
                                    ? parseEnum(MaritalStatus.class, req.getMaritalStatus(), "maritalStatus")
                                    : null;
    NomineeRelation relation    = parseEnum(NomineeRelation.class,  req.getNomineeRelation(), "nomineeRelation");

    LocalDate dob = LocalDate.parse(req.getDateOfBirth());

    Application app = Application.builder()
            .applicationId(applicationIdGenerator.generate())
            .accountType(accountType)
            .fullName(req.getFullName().trim())
            .fatherName(req.getFatherName().trim())
            .motherName(req.getMotherName().trim())
            .dateOfBirth(dob)
            .gender(gender)
            .maritalStatus(marital)
            .nationality(req.getNationality())
            .occupation(req.getOccupation().trim())
            .annualIncome(req.getAnnualIncome())
            .phoneNumber(req.getPhoneNumber().trim())
            .emailId(req.getEmailId().trim().toLowerCase())
            .aadhaarNumber(req.getAadhaarNumber().trim())
            .panNumber(req.getPanNumber().trim().toUpperCase())
            .currentAddressLine(req.getCurrentAddressLine().trim())
            .currentCity(req.getCurrentCity().trim())
            .currentState(req.getCurrentState().trim())
            .currentPincode(req.getCurrentPincode().trim())
            .permanentAddressLine(req.getPermanentAddressLine().trim())
            .permanentCity(req.getPermanentCity().trim())
            .permanentState(req.getPermanentState().trim())
            .permanentPincode(req.getPermanentPincode().trim())
            .nomineeName(req.getNomineeName().trim())
            .nomineeRelation(relation)
            .nomineeAge(Integer.parseInt(req.getNomineeAge()))
            .nomineeMobileNumber(req.getNomineeMobileNumber().trim())
            .nomineeAddress(req.getNomineeAddress().trim())
            .aadhaarCardFile(req.getAadhaarCardFile().getBytes())
            .aadhaarCardFileName(req.getAadhaarCardFile().getOriginalFilename())
            .aadhaarCardFileType(req.getAadhaarCardFile().getContentType())
            .panCardFile(req.getPanCardFile().getBytes())
            .panCardFileName(req.getPanCardFile().getOriginalFilename())
            .panCardFileType(req.getPanCardFile().getContentType())
            .profilePhoto(req.getProfilePhoto().getBytes())
            .profilePhotoName(req.getProfilePhoto().getOriginalFilename())
            .profilePhotoType(req.getProfilePhoto().getContentType())
            .signatureImage(req.getSignatureImage().getBytes())
            .signatureImageName(req.getSignatureImage().getOriginalFilename())
            .signatureImageType(req.getSignatureImage().getContentType())
            .addressProofDocument(req.getAddressProofDocument().getBytes())
            .addressProofDocumentName(req.getAddressProofDocument().getOriginalFilename())
            .addressProofDocumentType(req.getAddressProofDocument().getContentType())
            .otpVerified(true)  // authenticated user — no OTP needed
            .status(ApplicationStatus.SUBMITTED)
            .build();

    // Optional
    if (req.getPassportFile() != null && !req.getPassportFile().isEmpty()) {
        app.setPassportFile(req.getPassportFile().getBytes());
        app.setPassportFileName(req.getPassportFile().getOriginalFilename());
        app.setPassportFileType(req.getPassportFile().getContentType());
    }
    if (req.getVoterIdFile() != null && !req.getVoterIdFile().isEmpty()) {
        app.setVoterIdFile(req.getVoterIdFile().getBytes());
        app.setVoterIdFileName(req.getVoterIdFile().getOriginalFilename());
        app.setVoterIdFileType(req.getVoterIdFile().getContentType());
    }

    Application saved = applicationRepository.save(app);
    sendConfirmationEmail(saved);

    log.info("Authenticated application submitted: {} by {}", saved.getApplicationId(), username);

    return buildResponse(saved);
}
}