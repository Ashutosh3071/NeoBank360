// src/main/java/com/neobank/service/TransactionService.java

package com.neobank.service;

import com.neobank.dto.transaction.*;
import com.neobank.entity.*;
import com.neobank.entity.Transaction.*;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.*;
import com.neobank.util.TransactionReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class TransactionService {

    // ── Injected via constructor — @Lazy breaks circular dependency ──
    private final TransactionRepository         transactionRepository;
    private final AccountRepository             accountRepository;
    private final UserRepository                userRepository;
    private final TransactionReferenceGenerator refGen;
    private final MailService                   mailService;
    private final RewardService                 rewardService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            

    // Manual constructor — @Lazy on RewardService breaks the cycle
    public TransactionService(
            TransactionRepository         transactionRepository,
            AccountRepository             accountRepository,
            UserRepository                userRepository,
            TransactionReferenceGenerator refGen,
            MailService                   mailService,
            @Lazy RewardService           rewardService) {

        this.transactionRepository = transactionRepository;
        this.accountRepository     = accountRepository;
        this.userRepository        = userRepository;
        this.refGen                = refGen;
        this.mailService           = mailService;
        this.rewardService         = rewardService;
    }

    // ─────────────────────────────────────────────
    //  ADMIN: DEPOSIT
    // ─────────────────────────────────────────────

    @Transactional
    public TransactionResponseDTO adminDeposit(
            AdminTransactionRequestDTO req, String adminUsername) {

        Account account = findAccount(req.getAccountNumber());
        validateAccountOperable(account);

        BigDecimal amount    = parseBigDecimal(req.getAmount(), "amount");
        BigDecimal balBefore = account.getBalance();

        account.setBalance(balBefore.add(amount));
        account.recalculateAvailableBalance();
        account.setLastTransactionAt(LocalDateTime.now());
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .referenceNumber(refGen.generate())
                .toAccount(account)
                .toAccountNumber(account.getAccountNumber())
                .amount(amount).netAmount(amount)
                .charges(BigDecimal.ZERO).currency("INR")
                .transactionType(TransactionType.DEPOSIT)
                .transactionMode(TransactionMode.ADMIN)
                .description(req.getDescription() != null
                        ? req.getDescription() : "Admin Deposit")
                .remarks(req.getRemarks())
                .toBalanceBefore(balBefore)
                .toBalanceAfter(account.getBalance())
                .status(TransactionStatus.SUCCESS)
                .initiatedByUsername(adminUsername)
                .initiatedByRole(InitiatedByRole.ADMIN)
                .processedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        log.info("Admin deposit: {} → {} ₹{}",
                adminUsername, account.getAccountNumber(), amount);

        notifyTransaction(account, saved, "credited");
        return buildResponse(saved, account.getAccountNumber());
    }

    // ─────────────────────────────────────────────
    //  ADMIN: WITHDRAW
    // ─────────────────────────────────────────────

    @Transactional
    public TransactionResponseDTO adminWithdraw(
            AdminTransactionRequestDTO req, String adminUsername) {

        Account account = findAccount(req.getAccountNumber());
        validateAccountOperable(account);

        BigDecimal amount = parseBigDecimal(req.getAmount(), "amount");

        if (!account.hasSufficientBalance(amount)) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    "INSUFFICIENT_BALANCE",
                    "Insufficient balance. Available: ₹" +
                    account.getAvailableBalance());
        }

        BigDecimal balBefore = account.getBalance();
        account.setBalance(balBefore.subtract(amount));
        account.recalculateAvailableBalance();
        account.setLastTransactionAt(LocalDateTime.now());
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .referenceNumber(refGen.generate())
                .fromAccount(account)
                .fromAccountNumber(account.getAccountNumber())
                .amount(amount).netAmount(amount)
                .charges(BigDecimal.ZERO).currency("INR")
                .transactionType(TransactionType.WITHDRAWAL)
                .transactionMode(TransactionMode.ADMIN)
                .description(req.getDescription() != null
                        ? req.getDescription() : "Admin Withdrawal")
                .remarks(req.getRemarks())
                .fromBalanceBefore(balBefore)
                .fromBalanceAfter(account.getBalance())
                .status(TransactionStatus.SUCCESS)
                .initiatedByUsername(adminUsername)
                .initiatedByRole(InitiatedByRole.ADMIN)
                .processedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        log.info("Admin withdrawal: {} → {} ₹{}",
                adminUsername, account.getAccountNumber(), amount);

        notifyTransaction(account, saved, "debited");
        return buildResponse(saved, account.getAccountNumber());
    }

    // ─────────────────────────────────────────────
    //  UPI TRANSFER
    //  NOTE: This is for the legacy transfer-upi flow
    //  that uses account-based UPI (accountNumber@neobank).
    //  The new UpiService.pay() handles PIN-secured UPI
    //  payments via UpiId entities.
    //  Both can coexist — this one is simpler (no PIN).
    // ─────────────────────────────────────────────

    @Transactional
    public TransactionResponseDTO upiTransfer(
            UpiRequestDTO req, String username) {

        Account from = findAccount(req.getFromAccountNumber());
        validateAccountOperable(from);
        validateOwnership(from, username);

        if (!isValidUpiId(req.getUpiId())) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_UPI_ID",
                    "Invalid UPI ID format. Use: name@neobank");
        }

        Account to = resolveUpiAccount(req.getUpiId(), from);

        BigDecimal amount = parseBigDecimal(req.getAmount(), "amount");
        validateAmount(amount, from, "UPI");
        checkDailyLimit(from, amount);

        BigDecimal fromBefore = from.getBalance();
        BigDecimal toBefore   = to.getBalance();

        from.setBalance(fromBefore.subtract(amount));
        to.setBalance(toBefore.add(amount));
        from.recalculateAvailableBalance();
        to.recalculateAvailableBalance();
        from.setLastTransactionAt(LocalDateTime.now());
        to.setLastTransactionAt(LocalDateTime.now());

        accountRepository.save(from);
        accountRepository.save(to);

        String upiRef = "UPI" + System.currentTimeMillis();

        Transaction txn = Transaction.builder()
                .referenceNumber(refGen.generate())
                .fromAccount(from).toAccount(to)
                .fromAccountNumber(from.getAccountNumber())
                .toAccountNumber(to.getAccountNumber())
                .amount(amount).netAmount(amount)
                .charges(BigDecimal.ZERO).currency("INR")
                .transactionType(TransactionType.UPI)
                .transactionMode(TransactionMode.ONLINE)
                .upiId(req.getUpiId())
                .upiReference(upiRef)
                .description(req.getDescription() != null
                        ? req.getDescription() : "UPI Payment")
                .fromBalanceBefore(fromBefore)
                .fromBalanceAfter(from.getBalance())
                .toBalanceBefore(toBefore)
                .toBalanceAfter(to.getBalance())
                .status(TransactionStatus.SUCCESS)
                .initiatedByUsername(username)
                .initiatedByRole(InitiatedByRole.CUSTOMER)
                .processedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        log.info("UPI: {} → {} ₹{}",
                from.getAccountNumber(), req.getUpiId(), amount);

        notifyTransaction(from, saved, "debited");
        notifyTransaction(to,   saved, "credited");
        awardPoints(from.getUser().getId(), amount, saved.getReferenceNumber());

        return buildResponse(saved, from.getAccountNumber());
    }

    // ─────────────────────────────────────────────
    //  NEFT / RTGS TRANSFER
    // ─────────────────────────────────────────────

    @Transactional
    public TransactionResponseDTO neftTransfer(
            NeftRequestDTO req, String username) {

        Account from = findAccount(req.getFromAccountNumber());
        validateAccountOperable(from);
        validateOwnership(from, username);

        if (!req.getBeneficiaryIfsc().trim().toUpperCase()
                .matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_IFSC",
                    "Invalid IFSC format. Example: SBIN0001234");
        }

        BigDecimal amount = parseBigDecimal(req.getAmount(), "amount");
        validateAmount(amount, from, "NEFT");

        BigDecimal fromBefore = from.getBalance();
        from.setBalance(fromBefore.subtract(amount));
        from.recalculateAvailableBalance();
        from.setLastTransactionAt(LocalDateTime.now());
        accountRepository.save(from);

        // Try to find internal account
        Account toAccount = accountRepository
                .findByAccountNumber(req.getBeneficiaryAccountNumber())
                .orElse(null);

        TransactionType type = "RTGS".equalsIgnoreCase(req.getMode())
                ? TransactionType.RTGS
                : TransactionType.NEFT;

        Transaction txn = Transaction.builder()
                .referenceNumber(refGen.generate())
                .fromAccount(from)
                .fromAccountNumber(from.getAccountNumber())
                .toAccount(toAccount)
                .toAccountNumber(req.getBeneficiaryAccountNumber())
                .amount(amount).netAmount(amount)
                .charges(BigDecimal.ZERO).currency("INR")
                .transactionType(type)
                .transactionMode(TransactionMode.ONLINE)
                .beneficiaryName(req.getBeneficiaryName())
                .beneficiaryAccountNumber(req.getBeneficiaryAccountNumber())
                .beneficiaryIfsc(req.getBeneficiaryIfsc().trim().toUpperCase())
                .beneficiaryBankName(req.getBeneficiaryBankName())
                .description(req.getDescription() != null
                        ? req.getDescription() : type.name() + " Transfer")
                .fromBalanceBefore(fromBefore)
                .fromBalanceAfter(from.getBalance())
                .status(TransactionStatus.SUCCESS)
                .initiatedByUsername(username)
                .initiatedByRole(InitiatedByRole.CUSTOMER)
                .processedAt(LocalDateTime.now())
                .build();

        // Credit internal account if found
        if (toAccount != null) {
            BigDecimal toBefore = toAccount.getBalance();
            toAccount.setBalance(toBefore.add(amount));
            toAccount.recalculateAvailableBalance();
            toAccount.setLastTransactionAt(LocalDateTime.now());
            accountRepository.save(toAccount);
            txn.setToBalanceBefore(toBefore);
            txn.setToBalanceAfter(toAccount.getBalance());
        }

        Transaction saved = transactionRepository.save(txn);
        log.info("NEFT: {} → {} ₹{}",
                from.getAccountNumber(), req.getBeneficiaryAccountNumber(), amount);

        notifyTransaction(from, saved, "debited");
        awardPoints(from.getUser().getId(), amount, saved.getReferenceNumber());

        return buildResponse(saved, from.getAccountNumber());
    }

    // ─────────────────────────────────────────────
    //  SELF TRANSFER
    // ─────────────────────────────────────────────

    @Transactional
    public TransactionResponseDTO selfTransfer(
            SelfTransferRequestDTO req, String username) {

        Account from = findAccount(req.getFromAccountNumber());
        Account to   = findAccount(req.getToAccountNumber());

        validateAccountOperable(from);
        validateAccountOperable(to);
        validateOwnership(from, username);
        validateOwnership(to,   username);

        if (from.getId().equals(to.getId())) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT",
                    "Source and destination accounts cannot be the same.");
        }

        BigDecimal amount = parseBigDecimal(req.getAmount(), "amount");
        validateAmount(amount, from, "TRANSFER");

        BigDecimal fromBefore = from.getBalance();
        BigDecimal toBefore   = to.getBalance();

        from.setBalance(fromBefore.subtract(amount));
        to.setBalance(toBefore.add(amount));
        from.recalculateAvailableBalance();
        to.recalculateAvailableBalance();
        from.setLastTransactionAt(LocalDateTime.now());
        to.setLastTransactionAt(LocalDateTime.now());

        accountRepository.save(from);
        accountRepository.save(to);

        Transaction txn = Transaction.builder()
                .referenceNumber(refGen.generate())
                .fromAccount(from).toAccount(to)
                .fromAccountNumber(from.getAccountNumber())
                .toAccountNumber(to.getAccountNumber())
                .amount(amount).netAmount(amount)
                .charges(BigDecimal.ZERO).currency("INR")
                .transactionType(TransactionType.TRANSFER)
                .transactionMode(TransactionMode.ONLINE)
                .description(req.getDescription() != null
                        ? req.getDescription() : "Self Transfer")
                .fromBalanceBefore(fromBefore)
                .fromBalanceAfter(from.getBalance())
                .toBalanceBefore(toBefore)
                .toBalanceAfter(to.getBalance())
                .status(TransactionStatus.SUCCESS)
                .initiatedByUsername(username)
                .initiatedByRole(InitiatedByRole.CUSTOMER)
                .processedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        log.info("Self Transfer: {} → {} ₹{}",
                from.getAccountNumber(), to.getAccountNumber(), amount);

        // Self-transfer: no reward points (no external movement of money)
        return buildResponse(saved, from.getAccountNumber());
    }

    // ─────────────────────────────────────────────
    //  GET TRANSACTIONS
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getAccountTransactions(
            String accountNumber, String username, Pageable pageable) {

        Account account = findAccount(accountNumber);
        validateOwnership(account, username);
        return transactionRepository
                .findByAccountId(account.getId(), pageable)
                .map(t -> buildResponse(t, accountNumber));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(t -> buildResponse(t, null));
    }

    // ─────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private Account findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountNumber));
    }

    private void validateAccountOperable(Account account) {
        if (!account.isOperable()) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    "ACCOUNT_NOT_OPERABLE",
                    "Account " + account.getAccountNumber() +
                    " is " + account.getStatus().name());
        }
    }

    private void validateOwnership(Account account, String username) {
        if (!account.getUser().getUsername().equals(username)) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "You don't have access to this account.");
        }
    }

    private BigDecimal parseBigDecimal(String val, String field) {
        try {
            BigDecimal bd = new BigDecimal(val);
            if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NeoBankException(HttpStatus.BAD_REQUEST,
                        "INVALID_" + field.toUpperCase(),
                        field + " must be greater than 0.");
            }
            return bd;
        } catch (NumberFormatException e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    "INVALID_" + field.toUpperCase(),
                    "Invalid " + field + " value.");
        }
    }

    private void validateAmount(BigDecimal amount, Account from, String type) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    "INVALID_AMOUNT", "Amount must be greater than 0.");
        }
        if (!from.hasSufficientBalance(amount)) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    "INSUFFICIENT_BALANCE",
                    "Insufficient balance. Available: ₹" +
                    from.getAvailableBalance());
        }
        if (from.getPerTransactionLimit() != null &&
            amount.compareTo(from.getPerTransactionLimit()) > 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    "LIMIT_EXCEEDED",
                    "Amount ₹" + amount + " exceeds per-transaction limit ₹" +
                    from.getPerTransactionLimit());
        }
    }

    private void checkDailyLimit(Account from, BigDecimal amount) {
        BigDecimal todayTotal = transactionRepository
                .getTodayTotal(from.getId(), LocalDate.now());
        if (todayTotal.add(amount)
                      .compareTo(from.getDailyTransferLimit()) > 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    "DAILY_LIMIT_EXCEEDED",
                    "Daily transfer limit exceeded. " +
                    "Used: ₹" + todayTotal +
                    " / Limit: ₹" + from.getDailyTransferLimit());
        }
    }

    private Account resolveUpiAccount(String upiId, Account from) {
        String handle = upiId.split("@")[0];

        // Try by account number
        return accountRepository.findByAccountNumber(handle)
                .map(to -> {
                    if (to.getId().equals(from.getId())) {
                        throw new NeoBankException(
                                HttpStatus.BAD_REQUEST,
                                "SELF_TRANSFER_NOT_ALLOWED",
                                "Cannot send UPI payment to your own account.");
                    }
                    return to;
                })
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "UPI_NOT_FOUND",
                        "No NeoBank account linked to UPI ID: " + upiId));
    }

    private boolean isValidUpiId(String upiId) {
        return upiId != null
            && !upiId.isBlank()
            && upiId.matches("^[a-zA-Z0-9._\\-]+@[a-zA-Z0-9]+$");
    }

    // Award reward points — fire-and-forget, never throws
    private void awardPoints(Long userId, BigDecimal amount,
                             String txnRef) {
        try {
            rewardService.awardTransactionPoints(userId, amount);
        } catch (Exception e) {
            log.warn("Reward award failed for txn: {}", txnRef);
        }
    }

    private void notifyTransaction(Account account, Transaction txn,
                                   String direction) {
        try {
            String name = account.getUser().getFullName() != null
                    ? account.getUser().getFullName()
                    : "Customer";
            String balance = "credited".equals(direction)
                    ? (txn.getToBalanceAfter() != null
                        ? txn.getToBalanceAfter().toPlainString() : "—")
                    : (txn.getFromBalanceAfter() != null
                        ? txn.getFromBalanceAfter().toPlainString() : "—");

            mailService.sendMail(
                account.getUser().getEmail(),
                "NeoBank — Transaction Alert",
                """
                Dear %s,

                ₹%s has been %s your account.

                Reference : %s
                Type      : %s
                Amount    : ₹%s
                Balance   : ₹%s
                Date/Time : %s

                NeoBank Team
                """.formatted(
                    name,
                    txn.getAmount(),
                    direction,
                    txn.getReferenceNumber(),
                    txn.getTransactionType(),
                    txn.getAmount(),
                    balance,
                    txn.getProcessedAt().format(FMT)
                )
            );
        } catch (Exception e) {
            log.warn("Transaction email failed: {}",
                    txn.getReferenceNumber());
        }
    }

    private TransactionResponseDTO buildResponse(
            Transaction t, String perspectiveAccount) {

        String balAfter = null;
        if (perspectiveAccount != null) {
            if (perspectiveAccount.equals(t.getFromAccountNumber())
                && t.getFromBalanceAfter() != null) {
                balAfter = t.getFromBalanceAfter().toPlainString();
            } else if (perspectiveAccount.equals(t.getToAccountNumber())
                       && t.getToBalanceAfter() != null) {
                balAfter = t.getToBalanceAfter().toPlainString();
            }
        }

        return TransactionResponseDTO.builder()
                .id(t.getId())
                .referenceNumber(t.getReferenceNumber())
                .transactionType(t.getTransactionType().name())
                .transactionMode(t.getTransactionMode().name())
                .amount(t.getAmount().toPlainString())
                .charges(t.getCharges() != null
                        ? t.getCharges().toPlainString() : "0")
                .netAmount(t.getNetAmount() != null
                        ? t.getNetAmount().toPlainString()
                        : t.getAmount().toPlainString())
                .fromAccountNumber(t.getFromAccountNumber())
                .toAccountNumber(t.getToAccountNumber())
                .beneficiaryName(t.getBeneficiaryName())
                .upiId(t.getUpiId())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .balanceAfter(balAfter)
                .createdAt(t.getCreatedAt() != null
                        ? t.getCreatedAt().format(FMT) : null)
                .processedAt(t.getProcessedAt() != null
                        ? t.getProcessedAt().format(FMT) : null)
                .build();
    }
}