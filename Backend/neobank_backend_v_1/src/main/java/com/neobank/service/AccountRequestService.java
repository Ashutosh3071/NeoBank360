// src/main/java/com/neobank/service/AccountRequestService.java

package com.neobank.service;

import com.neobank.dto.accountrequest.*;
import com.neobank.entity.*;
import com.neobank.entity.AccountRequest.RequestStatus;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.*;
import com.neobank.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRequestService {

    private final AccountRequestRepository accountRequestRepository;
    private final UserRepository           userRepository;
    private final AccountRepository        accountRepository;
    private final ApplicationRepository    applicationRepository;
    private final AccountService           accountService;
    private final MailService              mailService;
    private final AccountNumberGenerator   accountNumberGenerator;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── Customer: Submit request ──
    @Transactional
    public AccountRequestResponseDTO submitRequest(
            AccountRequestDTO req, String username) {

        User user = findUser(username);

        // Must already have at least one account
        List<Account> existing = accountRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());
        if (existing.isEmpty()) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "NO_EXISTING_ACCOUNT",
                    "You must have an active account before requesting another.");
        }

        // No duplicate pending
        if (accountRequestRepository.hasPendingRequest(user.getId())) {
            throw new NeoBankException(HttpStatus.CONFLICT, "PENDING_REQUEST_EXISTS",
                    "You already have a pending account request. Please wait for review.");
        }

        Account.AccountType type;
        try {
            type = Account.AccountType.valueOf(req.getAccountType().toUpperCase());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_TYPE",
                    "Invalid account type.");
        }

        // Generate request ID
        String requestId = "REQ" + LocalDate.now().getYear()
                + String.format("%07d", accountRequestRepository.count() + 1);

        AccountRequest request = AccountRequest.builder()
                .requestId(requestId)
                .user(user)
                .accountType(type)
                .reason(req.getReason())
                .status(RequestStatus.PENDING)
                .build();

        AccountRequest saved = accountRequestRepository.save(request);

        // Email admin and user
        mailService.sendMail(user.getEmail(),
                "NeoBank — Account Request Submitted",
                "Dear " + (user.getFullName() != null ? user.getFullName() : user.getUsername()) +
                ",\n\nYour request for an additional " + type.name() +
                " account has been received.\n\nRequest ID: " + requestId +
                "\n\nWe will review and respond within 2-3 business days.\n\nNeoBank Team");

        return buildResponse(saved);
    }

    // ── Customer: Get my requests ──
    public Page<AccountRequestResponseDTO> getMyRequests(
            String username, Pageable pageable) {
        User user = findUser(username);
        return accountRequestRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::buildResponse);
    }

    // ── Admin: Get all requests ──
    public Page<AccountRequestResponseDTO> getAllRequests(
            RequestStatus status, Pageable pageable) {
        Page<AccountRequest> page = (status != null)
                ? accountRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : accountRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(this::buildDetailedResponse);
    }

    // ── Admin: Get one request detail ──
    public AccountRequestResponseDTO getRequestDetail(String requestId) {
        AccountRequest req = findRequest(requestId);
        return buildDetailedResponse(req);
    }

    // ── Admin: Approve ──
    @Transactional
    public AccountRequestResponseDTO approveRequest(
            ReviewAccountRequestDTO dto, String adminUsername) {

        AccountRequest req = findRequest(dto.getRequestId());

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "ALREADY_REVIEWED",
                    "This request has already been " + req.getStatus().name().toLowerCase());
        }

        // Create new account
        Account account = Account.builder()
                .accountNumber(accountNumberGenerator.generate())
                .user(req.getUser())
                .accountType(req.getAccountType())
                .accountCategory(Account.AccountCategory.INDIVIDUAL)
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .holdAmount(BigDecimal.ZERO)
                .minimumBalance(new BigDecimal("500"))
                .currency("INR")
                .branchName(dto.getBranchName() != null ? dto.getBranchName() : "Main Branch")
                .branchCode(dto.getBranchCode() != null ? dto.getBranchCode() : "001")
                .ifscCode(dto.getIfscCode()    != null ? dto.getIfscCode()    : "NEOB0000001")
                .micrCode("400000001")
                .interestRate(new BigDecimal("3.5"))
                .dailyTransferLimit(new BigDecimal("100000"))
                .dailyWithdrawalLimit(new BigDecimal("50000"))
                .perTransactionLimit(new BigDecimal("100000"))
                .status(Account.AccountStatus.ACTIVE)
                .openedOn(LocalDate.now())
                .netBankingEnabled(true)
                .mobileBankingEnabled(true)
                .chequeBookEnabled(false)
                .upiEnabled(true)
                .debitCardEnabled(false)
                .smsAlertsEnabled(true)
                .emailAlertsEnabled(true)
                .build();

        Account savedAccount = accountRepository.save(account);

        req.setStatus(RequestStatus.APPROVED);
        req.setReviewedBy(adminUsername);
        req.setReviewedAt(LocalDateTime.now());
        req.setCreatedAccount(savedAccount);
        req.setBranchName(dto.getBranchName());
        req.setBranchCode(dto.getBranchCode());
        req.setIfscCode(dto.getIfscCode());
        accountRequestRepository.save(req);

        log.info("Account request {} approved by {} → {}", req.getRequestId(), adminUsername, savedAccount.getAccountNumber());

        mailService.sendMail(req.getUser().getEmail(),
                "NeoBank — Account Request Approved!",
                "Dear " + (req.getUser().getFullName() != null ? req.getUser().getFullName() : req.getUser().getUsername()) +
                ",\n\nGreat news! Your request for an additional " + req.getAccountType().name() +
                " account has been approved.\n\nNew Account Number: " + savedAccount.getAccountNumber() +
                "\nIFSC Code: " + savedAccount.getIfscCode() +
                "\nBranch: " + savedAccount.getBranchName() +
                "\n\nYou can now manage your new account in the NeoBank app.\n\nNeoBank Team");

        return buildDetailedResponse(req);
    }

    // ── Admin: Reject ──
    @Transactional
    public AccountRequestResponseDTO rejectRequest(
            ReviewAccountRequestDTO dto, String adminUsername) {

        AccountRequest req = findRequest(dto.getRequestId());

        if (req.getStatus() != RequestStatus.PENDING) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "ALREADY_REVIEWED",
                    "This request has already been reviewed.");
        }

        req.setStatus(RequestStatus.REJECTED);
        req.setReviewedBy(adminUsername);
        req.setReviewedAt(LocalDateTime.now());
        req.setRejectionReason(dto.getRejectionReason());
        accountRequestRepository.save(req);

        mailService.sendMail(req.getUser().getEmail(),
                "NeoBank — Account Request Update",
                "Dear " + (req.getUser().getFullName() != null ? req.getUser().getFullName() : req.getUser().getUsername()) +
                ",\n\nYour request (ID: " + req.getRequestId() + ") for an additional account was not approved." +
                "\n\nReason: " + dto.getRejectionReason() +
                "\n\nFor assistance, contact support@neobank.in\n\nNeoBank Team");

        return buildDetailedResponse(req);
    }

    // ── Helpers ──
    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private AccountRequest findRequest(String requestId) {
        return accountRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND",
                        "Account request not found: " + requestId));
    }

    private AccountRequestResponseDTO buildResponse(AccountRequest r) {
        return AccountRequestResponseDTO.builder()
                .id(r.getId())
                .requestId(r.getRequestId())
                .accountType(r.getAccountType().name())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .rejectionReason(r.getRejectionReason())
                .reviewedBy(r.getReviewedBy())
                .reviewedAt(r.getReviewedAt() != null ? r.getReviewedAt().format(FMT) : null)
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : null)
                .createdAccountNumber(r.getCreatedAccount() != null ? r.getCreatedAccount().getAccountNumber() : null)
                .username(r.getUser().getUsername())
                .fullName(r.getUser().getFullName())
                .email(r.getUser().getEmail())
                .build();
    }

    @SuppressWarnings("unchecked")
    private AccountRequestResponseDTO buildDetailedResponse(AccountRequest r) {
        AccountRequestResponseDTO dto = buildResponse(r);
        dto.setUserId(String.valueOf(r.getUser().getId()));
        dto.setPhone(r.getUser().getPhone());

        // Existing accounts
        List<Account> accounts = accountRepository
                .findByUserIdOrderByCreatedAtDesc(r.getUser().getId());
        dto.setExistingAccounts((List<Object>)(List<?>)
                accounts.stream().map(accountService::buildResponse).toList());

        // Latest application
        applicationRepository
                .findByEmailIdOrderByCreatedAtDesc(r.getUser().getEmail())
                .stream().findFirst()
                .ifPresent(app -> dto.setLatestApplication(Map.of(
                        "applicationId", app.getApplicationId(),
                        "status", app.getStatus().name(),
                        "accountType", app.getAccountType().name(),
                        "submittedOn", app.getCreatedAt() != null
                                ? app.getCreatedAt().format(FMT) : ""
                )));

        return dto;
    }
}