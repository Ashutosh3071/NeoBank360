package com.neobank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.LoanRepaymentDTO;
import com.neobank.entity.Account;
import com.neobank.entity.LoanAccount;
import com.neobank.entity.LoanRepayment;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.RepaymentStatus;
import com.neobank.enums.Role;
import com.neobank.enums.TransactionType;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.LoanAccountRepository;
import com.neobank.repository.LoanRepaymentRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;

@Service
public class LoanRepaymentService {

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private UserRepository userRepository;

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
    public void generateScheduleInternal(LoanAccount account) {
        BigDecimal principal = account.getPrincipalAmount();
        BigDecimal annualInterestRate = account.getAnnualInterestRate();
        int tenure = account.getTenureMonths();
        BigDecimal emi = account.getEmiAmount();

        double monthlyRate = annualInterestRate.doubleValue() / 12.0 / 100.0;
        BigDecimal outstanding = principal;
        LocalDate disbursedDate = account.getDisbursedAt() != null ?
                account.getDisbursedAt().toLocalDate() : LocalDate.now();

        for (int i = 1; i <= tenure; i++) {
            BigDecimal interest = outstanding.multiply(BigDecimal.valueOf(monthlyRate))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal prComponent;
            BigDecimal finalEmi;

            if (i == tenure) {
                // Final instalment absorbs rounding residual
                prComponent = outstanding;
                finalEmi = prComponent.add(interest).setScale(2, RoundingMode.HALF_UP);
            } else {
                prComponent = emi.subtract(interest).setScale(2, RoundingMode.HALF_UP);
                finalEmi = emi;
            }

            outstanding = outstanding.subtract(prComponent);

            LoanRepayment repayment = new LoanRepayment();
            repayment.setLoanAccount(account);
            repayment.setInstalmentNumber(i);
            repayment.setDueDate(disbursedDate.plusMonths(i));
            repayment.setEmiAmount(finalEmi);
            repayment.setPrincipalComponent(prComponent);
            repayment.setInterestComponent(interest);
            repayment.setPaymentStatus(RepaymentStatus.PENDING);

            loanRepaymentRepository.save(repayment);
        }
    }

    @Transactional
    public void updateOverdueStatusForAccount(Long loanAccountId) {
        List<LoanRepayment> repayments = loanRepaymentRepository.findAllByLoanAccountId(loanAccountId);
        LocalDate today = LocalDate.now();
        boolean changed = false;
        for (LoanRepayment repayment : repayments) {
            if (repayment.getPaymentStatus() == RepaymentStatus.PENDING &&
                repayment.getDueDate().isBefore(today)) {
                repayment.setPaymentStatus(RepaymentStatus.OVERDUE);
                loanRepaymentRepository.save(repayment);
                changed = true;
            }
        }
    }

    @Transactional
    public Page<LoanRepaymentDTO> getRepaymentsForAccount(Long loanAccountId, String status, int page, int size) {
        User user = getAuthenticatedUser();

        LoanAccount account = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan account not found"));

        // Validate Ownership: must be account owner or ADMIN
        if (!account.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this repayment schedule");
        }

        // Dynamically update overdue items before fetching
        updateOverdueStatusForAccount(loanAccountId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "instalmentNumber"));
        Page<LoanRepayment> repayments;

        if (status != null && !status.trim().isEmpty()) {
            try {
                RepaymentStatus enumStatus = RepaymentStatus.valueOf(status.toUpperCase());
                repayments = loanRepaymentRepository.findAllByLoanAccountIdAndPaymentStatus(loanAccountId, enumStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment status parameter");
            }
        } else {
            repayments = loanRepaymentRepository.findAllByLoanAccountId(loanAccountId, pageable);
        }

        return repayments.map(this::toDTO);
    }

    @Transactional
    public LoanRepaymentDTO payInstallment(Long loanAccountId, Long repaymentId, Long accountId) {
        User user = getAuthenticatedUser();

        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan account not found"));

        // Validate Ownership: Only the loan account owner can pay
        if (!loanAccount.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this loan account");
        }

        LoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repayment installment not found"));

        if (!repayment.getLoanAccount().getId().equals(loanAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repayment instalment does not belong to this loan account");
        }

        if (repayment.getPaymentStatus() == RepaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Instalment is already paid");
        }

        // --- Debit the selected financial account ---
        Account financialAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected financial account not found."));

        if (!financialAccount.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this financial account");
        }

        if (!Boolean.TRUE.equals(financialAccount.getIsActive()) || financialAccount.getAccountStatus() != AccountStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Selected financial account is not approved or is inactive.");
        }

        BigDecimal emiAmount = repayment.getEmiAmount();
        if (financialAccount.getBalance().compareTo(emiAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance in the selected financial account.");
        }

        // Deduct balance
        financialAccount.setBalance(financialAccount.getBalance().subtract(emiAmount));
        accountRepository.save(financialAccount);

        // Record DEBIT transaction
        Transaction tx = new Transaction();
        tx.setAccount(financialAccount);
        tx.setTransactionType(TransactionType.DEBIT);
        tx.setAmount(emiAmount);
        tx.setBalanceAfter(financialAccount.getBalance());
        tx.setDescription("Loan EMI Payment - Installment #" + repayment.getInstalmentNumber() + " (Loan Account " + formatLoanAccountNumber(loanAccountId) + ")");
        tx.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(tx);

        // Mark repayment as PAID
        repayment.setPaymentStatus(RepaymentStatus.PAID);
        repayment.setPaidAt(LocalDateTime.now());

        LoanRepayment saved = loanRepaymentRepository.save(repayment);
        return toDTO(saved);
    }

    private String formatLoanAccountNumber(Long accountId) {
        if (accountId == null) return "";
        return "LN-" + String.format("%06d", accountId);
    }

    public LoanRepaymentDTO toDTO(LoanRepayment r) {
        return new LoanRepaymentDTO(
                r.getId(),
                r.getLoanAccount().getId(),
                r.getInstalmentNumber(),
                r.getDueDate(),
                r.getEmiAmount(),
                r.getPrincipalComponent(),
                r.getInterestComponent(),
                r.getPaymentStatus(),
                r.getPaidAt()
        );
    }
}
