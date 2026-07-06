package com.neobank.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityNotFoundException;

import com.neobank.dto.LoanApplicationRequestDTO;
import com.neobank.dto.LoanApplicationResponseDTO;
import com.neobank.dto.LoanDecisionDTO;
import com.neobank.entity.LoanApplication;
import com.neobank.entity.LoanProduct;
import com.neobank.entity.User;
import com.neobank.entity.Account;
import com.neobank.entity.Transaction;
import com.neobank.enums.LoanStatus;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.TransactionType;
import com.neobank.repository.LoanApplicationRepository;
import com.neobank.repository.LoanProductRepository;
import com.neobank.repository.UserRepository;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.TransactionRepository;

@Service
public class LoanApplicationService {

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanAccountService loanAccountService;

    @Autowired
    private LoanRepaymentService loanRepaymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        Object principal = authentication.getPrincipal();
        String email;
        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String str && !"anonymousUser".equals(str)) {
            email = str;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    @Transactional
    public LoanApplicationResponseDTO apply(LoanApplicationRequestDTO request) {
        User user = getAuthenticatedUser();

        LoanProduct product = loanProductRepository.findById(request.getLoanProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Loan Product ID"));

        // Validate Amount
        if (request.getRequestedAmount() == null ||
            request.getRequestedAmount().compareTo(product.getMinAmount()) < 0 ||
            request.getRequestedAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Requested amount must fall strictly within the minimum and maximum limits of the selected loan product");
        }

        // Validate Tenure
        String[] allowed = product.getAllowedTenures().split(",");
        String reqTenureStr = String.valueOf(request.getRequestedTenureMonths());
        boolean tenureValid = Arrays.stream(allowed)
                .map(String::trim)
                .anyMatch(t -> t.equals(reqTenureStr));

        if (!tenureValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Requested tenure must exactly match one of the allowed tenures configured for the selected product");
        }

        // Check for Duplicate PENDING application
        boolean duplicateExists = loanApplicationRepository.existsByUserIdAndLoanProductIdAndStatus(
                user.getId(), product.getId(), LoanStatus.PENDING);
        if (duplicateExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A duplicate application for the same product while a PENDING application already exists is not allowed");
        }

        // Retrieve and validate Account
        if (request.getDisbursementAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Disbursement Account ID is required");
        }
        Account account = accountRepository.findById(request.getDisbursementAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Disbursement Account ID"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Disbursement account must belong to you");
        }

        if (!Boolean.TRUE.equals(account.getIsActive()) || account.getAccountStatus() != AccountStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Disbursement account is not active or approved by admin");
        }

        LoanApplication app = new LoanApplication();
        app.setUser(user);
        app.setLoanProduct(product);
        app.setRequestedAmount(request.getRequestedAmount());
        app.setRequestedTenureMonths(request.getRequestedTenureMonths());
        app.setDisbursementAccount(account);
        app.setStatus(LoanStatus.PENDING);

        LoanApplication saved = loanApplicationRepository.save(app);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponseDTO> getMyApplications() {
        User user = getAuthenticatedUser();
        return loanApplicationRepository.findAllByUserId(user.getId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponseDTO> getAdminApplications(LoanStatus status) {
        List<LoanApplication> list;
        if (status != null) {
            list = loanApplicationRepository.findAllByStatus(status);
        } else {
            list = loanApplicationRepository.findAll();
        }
        return list.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanApplicationResponseDTO decide(Long id, LoanDecisionDTO request) {
        LoanApplication app = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found"));

        if (app.getStatus() != LoanStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application has already been decided");
        }

        app.setStatus(request.getDecision());
        app.setDecidedAt(LocalDateTime.now());
        app.setAdminRemarks(request.getAdminRemarks());

        LoanApplication saved = loanApplicationRepository.save(app);

        if (request.getDecision() == LoanStatus.APPROVED) {
            // Atomic creation of Loan Account and Repayment Schedule
            var account = loanAccountService.createAccountInternal(saved);
            loanRepaymentService.generateScheduleInternal(account);

            // Disburse the amount to the selected bank account!
            // Safely resolve the disbursement account — old rows may have id=0 (invalid proxy)
            Account disbursementAccount = resolveDisbursementAccount(saved);
            if (disbursementAccount != null) {
                disbursementAccount.setBalance(disbursementAccount.getBalance().add(saved.getRequestedAmount()));
                accountRepository.save(disbursementAccount);

                // Create a CREDIT transaction record
                Transaction tx = new Transaction();
                tx.setAccount(disbursementAccount);
                tx.setTransactionType(TransactionType.CREDIT);
                tx.setAmount(saved.getRequestedAmount());
                tx.setBalanceAfter(disbursementAccount.getBalance());
                tx.setDescription("Loan Disbursement: " + saved.getLoanProduct().getProductName() + " - Ref: LA-" + saved.getId());
                tx.setTransactionDate(LocalDateTime.now());
                transactionRepository.save(tx);
            }
        }

        return toDTO(saved);
    }

    /**
     * Safely resolves the disbursement account for a LoanApplication.
     * Uses a native query to read the raw FK column value, completely bypassing
     * Hibernate proxy issues (old rows may have disbursement_account_id = 0).
     */
    private Account resolveDisbursementAccount(LoanApplication app) {
        try {
            Long accountId = loanApplicationRepository.findDisbursementAccountIdById(app.getId());
            if (accountId == null || accountId <= 0) return null;
            return accountRepository.findById(accountId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public LoanApplicationResponseDTO toDTO(LoanApplication app) {
        LoanApplicationResponseDTO dto = new LoanApplicationResponseDTO(
                app.getId(),
                app.getUser().getId(),
                app.getUser().getEmail(),
                app.getUser().getFullName(),
                app.getLoanProduct().getId(),
                app.getLoanProduct().getProductName(),
                app.getRequestedAmount(),
                app.getRequestedTenureMonths(),
                app.getStatus(),
                app.getAdminRemarks(),
                app.getAppliedAt(),
                app.getDecidedAt()
        );
        // Safely resolve the disbursement account using native query
        Account da = resolveDisbursementAccount(app);
        if (da != null) {
            dto.setDisbursementAccountId(da.getId());
            dto.setDisbursementAccountNumber(da.getAccountNumber());
        }
        return dto;
    }
}
