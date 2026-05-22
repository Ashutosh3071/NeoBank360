// src/main/java/com/neobank/service/UpiService.java

package com.neobank.service;

import com.neobank.dto.upi.*;
import com.neobank.entity.*;
import com.neobank.entity.OtpVerification.OtpPurpose;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.*;
import com.neobank.util.TransactionReferenceGenerator;
import com.neobank.util.UpiReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpiService {

    private final UpiIdRepository           upiIdRepository;
    private final UpiTransactionRepository  upiTransactionRepository;
    private final AccountRepository         accountRepository;
    private final UserRepository            userRepository;
    private final TransactionRepository     transactionRepository;
    private final OtpService                otpService;
    private final PasswordEncoder           passwordEncoder;
    private final TransactionReferenceGenerator refGen;
    // Add to UpiService.java injected fields:
    private final MailService mailService;
    
 // Add to constructor parameters
    private final UpiReferenceGenerator upiRefGen;
    private final RewardService         rewardService;

    private static final String UPI_SUFFIX = "@neobank";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────
    //  CREATE UPI ID
    //  - Only prefix + account needed
    //  - No phone number
    //  - First UPI ID per account = primary automatically
    // ─────────────────────────────────────────────

    @Transactional
    public UpiIdResponseDTO createUpiId(CreateUpiRequestDTO req, String username) {

        User user    = findUser(username);
        Account acct = findAccount(req.getAccountNumber());
        validateOwnership(acct, username);

        // Clean prefix
        String prefix = req.getVpaPrefix().trim().toLowerCase()
                .replaceAll("[^a-z0-9._\\-]", "");

        if (prefix.length() < 3) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_VPA",
                    "UPI ID prefix must be at least 3 characters (a-z, 0-9, . _ -).");
        }
        if (prefix.length() > 30) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_VPA",
                    "UPI ID prefix must be at most 30 characters.");
        }

        String vpa = prefix + UPI_SUFFIX;

        // Duplicate check
        if (upiIdRepository.existsByVpa(vpa)) {
            throw new NeoBankException(HttpStatus.CONFLICT, "VPA_EXISTS",
                    "UPI ID '" + vpa + "' is already taken. Please try another prefix.");
        }

        // Max 5 UPI IDs per account
        long count = upiIdRepository.countActiveByAccountId(acct.getId());
        if (count >= 5) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "MAX_UPI_LIMIT",
                    "Maximum 5 UPI IDs allowed per account.");
        }

        // First UPI ID for this account becomes primary automatically
        boolean isPrimary = (count == 0);

        UpiId upiId = UpiId.builder()
                .vpa(vpa)
                .account(acct)
                .user(user)
                .pinSet(false)
                .status(UpiId.UpiStatus.ACTIVE)
                .isPrimary(isPrimary)
                .dailyLimit(100000L)
                .perTxnLimit(100000L)
                .build();

        UpiId saved = upiIdRepository.save(upiId);

        log.info("UPI ID created: {} → account: {} isPrimary: {}",
                vpa, acct.getAccountNumber(), isPrimary);

        return buildUpiResponse(saved);
    }

    // ─────────────────────────────────────────────
    //  VPA LOOKUP
    //  Supports:
    //   - Direct VPA:   john@neobank
    //   - Phone number: 9876543210  → resolves to primary UPI ID
    // ─────────────────────────────────────────────

    public VpaLookupResponseDTO lookupVpa(String query) {

        String q = query.trim();

        // ── Phone number format ──
        if (q.matches("^[6-9]\\d{9}$")) {
            return upiIdRepository.findPrimaryByUserPhone(q)
                    .map(u -> buildLookupResponse(u, q))
                    .orElse(VpaLookupResponseDTO.builder()
                            .query(q)
                            .valid(false)
                            .message("No NeoBank account linked to this mobile number.")
                            .build());
        }

        // ── VPA format ──
        if (q.contains("@")) {
            return upiIdRepository.findByVpaAndStatus(q, UpiId.UpiStatus.ACTIVE)
                    .map(u -> buildLookupResponse(u, q))
                    .orElse(VpaLookupResponseDTO.builder()
                            .query(q)
                            .valid(false)
                            .message("UPI ID not found or inactive.")
                            .build());
        }

        return VpaLookupResponseDTO.builder()
                .query(q)
                .valid(false)
                .message("Enter a valid UPI ID (e.g. name@neobank) or 10-digit mobile number.")
                .build();
    }

    // ─────────────────────────────────────────────
    //  RESOLVE VPA FROM QUERY
    //  Used internally in pay() to handle
    //  both VPA and phone number inputs
    // ─────────────────────────────────────────────

    private UpiId resolveReceiver(String query) {

        String q = query.trim();

        // Phone number
        if (q.matches("^[6-9]\\d{9}$")) {
            return upiIdRepository.findPrimaryByUserPhone(q)
                    .orElseThrow(() -> new NeoBankException(
                            HttpStatus.NOT_FOUND, "VPA_NOT_FOUND",
                            "No NeoBank account found for mobile: " + q +
                            ". Ask the recipient to link their account to UPI."
                    ));
        }

        // VPA
        if (q.contains("@")) {
            return upiIdRepository.findByVpaAndStatus(q, UpiId.UpiStatus.ACTIVE)
                    .orElseThrow(() -> new NeoBankException(
                            HttpStatus.NOT_FOUND, "VPA_NOT_FOUND",
                            "UPI ID not found: " + q
                    ));
        }

        throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_RECEIVER",
                "Enter a valid UPI ID or 10-digit mobile number.");
    }

    // ─────────────────────────────────────────────
    //  UPI PAY
    // ─────────────────────────────────────────────

    @Transactional
    public UpiTransactionResponseDTO pay(UpiPayRequestDTO req, String username) {
    	
        // ── Load sender UPI ID ──
        UpiId senderUpi = upiIdRepository
                .findByVpaAndStatus(req.getSenderVpa(), UpiId.UpiStatus.ACTIVE)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "VPA_NOT_FOUND",
                        "Your UPI ID not found: " + req.getSenderVpa()));

        validateOwnershipByUser(senderUpi, username);

        // ── PIN checks ──
        if (!senderUpi.getPinSet()) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PIN_NOT_SET",
                    "Please set your UPI PIN first.");
        }
        if (senderUpi.isPinLocked()) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PIN_LOCKED",
                    "UPI PIN is locked for 24 hours. Please reset your PIN.");
        }

        // ── Verify PIN ──
        if (!passwordEncoder.matches(req.getUpiPin(), senderUpi.getUpiPinHash())) {
            senderUpi.incrementFailedPin();
            upiIdRepository.save(senderUpi);

            int remaining = 3 - senderUpi.getFailedPinAttempts();
            if (remaining <= 0) {
                throw new NeoBankException(HttpStatus.BAD_REQUEST, "PIN_LOCKED",
                        "UPI PIN locked for 24 hours due to too many wrong attempts.");
            }
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "WRONG_PIN",
                    "Wrong UPI PIN. " + remaining + " attempt(s) remaining.");
        }

        // ── Resolve receiver (VPA or phone) ──
        UpiId receiverUpi = resolveReceiver(req.getReceiverVpa());

        // ── Cannot pay yourself ──
        if (senderUpi.getAccount().getId().equals(receiverUpi.getAccount().getId())) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "SELF_PAY",
                    "Cannot send money to your own account.");
        }

        // ── Amount ──
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.getAmount());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT",
                    "Invalid amount.");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT",
                    "Amount must be greater than ₹0.");
        }

        // ── Per-transaction limit ──
        if (amount.compareTo(new BigDecimal(senderUpi.getPerTxnLimit())) > 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "LIMIT_EXCEEDED",
                    "Amount exceeds per-transaction limit of ₹" +
                    senderUpi.getPerTxnLimit() + ".");
        }

        // ── Daily limit ──
        BigDecimal usedToday = upiTransactionRepository
                .getTodayUsedLimit(req.getSenderVpa(), LocalDate.now());

        if (usedToday.add(amount)
                     .compareTo(new BigDecimal(senderUpi.getDailyLimit())) > 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "DAILY_LIMIT_EXCEEDED",
                    "Daily UPI limit exceeded. Used: ₹" + usedToday +
                    " / Limit: ₹" + senderUpi.getDailyLimit() + ".");
        }

        // ── Balance check ──
        Account from = senderUpi.getAccount();
        Account to   = receiverUpi.getAccount();

        if (!from.hasSufficientBalance(amount)) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE",
                    "Insufficient balance. Available: ₹" + from.getAvailableBalance());
        }

        // ── Execute transfer ──
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

        // ── Reset PIN attempts on success ──
        senderUpi.resetPinAttempts();
        senderUpi.setLastUsedAt(LocalDateTime.now());
        upiIdRepository.save(senderUpi);

        // ── Reference ──
//        String upiRef = "UPI" + System.currentTimeMillis();
        String upiRef = upiRefGen.generate();

        // ── Save main Transaction record ──
        Transaction txn = Transaction.builder()
                .referenceNumber(refGen.generate())
                .fromAccount(from)
                .toAccount(to)
                .fromAccountNumber(from.getAccountNumber())
                .toAccountNumber(to.getAccountNumber())
                .amount(amount)
                .netAmount(amount)
                .charges(BigDecimal.ZERO)
                .currency("INR")
                .transactionType(Transaction.TransactionType.UPI)
                .transactionMode(Transaction.TransactionMode.ONLINE)
                .upiId(receiverUpi.getVpa())    // always store the VPA
                .upiReference(upiRef)
                .description(req.getDescription() != null
                        ? req.getDescription() : "UPI Payment")
                .fromBalanceBefore(fromBefore)
                .fromBalanceAfter(from.getBalance())
                .toBalanceBefore(toBefore)
                .toBalanceAfter(to.getBalance())
                .status(Transaction.TransactionStatus.SUCCESS)
                .initiatedByUsername(username)
                .initiatedByRole(Transaction.InitiatedByRole.CUSTOMER)
                .processedAt(LocalDateTime.now())
                .build();

        Transaction savedTxn = transactionRepository.save(txn);

        // ── Save UPI Transaction record ──
        UpiTransaction upiTxn = UpiTransaction.builder()
                .upiReference(upiRef)
                .transactionId(savedTxn.getReferenceNumber())
                .senderVpa(senderUpi.getVpa())
                .receiverVpa(receiverUpi.getVpa())   // always VPA, not phone
                .amount(amount)
                .description(req.getDescription())
                .status(UpiTransaction.UpiTxnStatus.SUCCESS)
                .senderUpiId(senderUpi)
                .receiverUpiId(receiverUpi)
                .build();

        upiTransactionRepository.save(upiTxn);

        log.info("UPI: {} → {} ₹{} Ref: {}",
                senderUpi.getVpa(), receiverUpi.getVpa(), amount, upiRef);
        
     // ── Award reward points (fire-and-forget) ──
        try {
            rewardService.awardTransactionPoints(
                    senderUpi.getAccount().getUser().getId(), amount);
        } catch (Exception e) {
            log.warn("Reward award failed for UPI ref: {}", upiRef);
        }

        // ── Notify ──
        notifyUpiTransaction(senderUpi, receiverUpi, upiTxn);

        return buildUpiTxnResponse(upiTxn, senderUpi.getVpa());
    }

    // ─────────────────────────────────────────────
    //  SEND PIN SETUP OTP
    // ─────────────────────────────────────────────

    public void sendPinSetupOtp(String vpa, String username) {
        UpiId upiId = findActiveVpa(vpa);
        validateOwnershipByUser(upiId, username);

        otpService.sendOtp(
                upiId.getUser().getEmail(),
                OtpPurpose.UPI_PIN_SETUP,
                "NeoBank UPI — PIN Setup OTP",
                """
                Dear Customer,
                
                Set UPI PIN for :  %s
                Your OTP to     : {OTP}

                Valid for 10 minutes.
                Do NOT share this OTP with anyone.

                NeoBank UPI Team
                """.formatted(vpa)
        );
    }

    // ─────────────────────────────────────────────
    //  SET UPI PIN
    // ─────────────────────────────────────────────

    @Transactional
    public void setUpiPin(SetUpiPinRequestDTO req, String username) {
        UpiId upiId = findActiveVpa(req.getVpa());
        validateOwnershipByUser(upiId, username);

        // Verify OTP
        otpService.verifyOtp(
                upiId.getUser().getEmail(),
                OtpPurpose.UPI_PIN_SETUP,
                req.getOtp()
        );

        validatePin(req.getNewPin(), req.getConfirmPin());

        upiId.setUpiPinHash(passwordEncoder.encode(req.getNewPin()));
        upiId.setPinSet(true);
        upiId.resetPinAttempts();
        upiIdRepository.save(upiId);

        log.info("UPI PIN set for: {}", req.getVpa());
    }

    // ─────────────────────────────────────────────
    //  CHANGE UPI PIN
    // ─────────────────────────────────────────────

    @Transactional
    public void changeUpiPin(ChangeUpiPinRequestDTO req, String username) {
        UpiId upiId = findActiveVpa(req.getVpa());
        validateOwnershipByUser(upiId, username);

        if (!upiId.getPinSet()) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PIN_NOT_SET",
                    "PIN not set yet. Use Set PIN instead.");
        }
        if (upiId.isPinLocked()) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PIN_LOCKED",
                    "PIN is locked for 24 hours.");
        }
        if (!passwordEncoder.matches(req.getCurrentPin(), upiId.getUpiPinHash())) {
            upiId.incrementFailedPin();
            upiIdRepository.save(upiId);
            int remaining = 3 - upiId.getFailedPinAttempts();
            throw new NeoBankException(HttpStatus.BAD_REQUEST,
                    remaining > 0 ? "WRONG_PIN" : "PIN_LOCKED",
                    remaining > 0
                        ? "Wrong PIN. " + remaining + " attempt(s) remaining."
                        : "PIN locked for 24 hours.");
        }

        validatePin(req.getNewPin(), req.getConfirmPin());

        upiId.setUpiPinHash(passwordEncoder.encode(req.getNewPin()));
        upiId.resetPinAttempts();
        upiIdRepository.save(upiId);
    }

    // ─────────────────────────────────────────────
    //  MY UPI IDs
    // ─────────────────────────────────────────────

    public List<UpiIdResponseDTO> getMyUpiIds(String accountNumber, String username) {
        Account acct = findAccount(accountNumber);
        validateOwnership(acct, username);
        return upiIdRepository
                .findByAccountIdAndStatusNot(acct.getId(), UpiId.UpiStatus.DELETED)
                .stream().map(this::buildUpiResponse).toList();
    }

    public List<UpiIdResponseDTO> getAllMyUpiIds(String username) {
        User user = findUser(username);
        return upiIdRepository
                .findByUserIdAndStatusNot(user.getId(), UpiId.UpiStatus.DELETED)
                .stream().map(this::buildUpiResponse).toList();
    }

    // ─────────────────────────────────────────────
    //  UPI TRANSACTIONS
    // ─────────────────────────────────────────────

    public Page<UpiTransactionResponseDTO> getUpiTransactions(
            String vpa, String username, Pageable pageable) {

        UpiId upiId = findActiveVpa(vpa);
        validateOwnershipByUser(upiId, username);

        return upiTransactionRepository
                .findBySenderVpaOrReceiverVpaOrderByCreatedAtDesc(
                        vpa, vpa, pageable)
                .map(t -> buildUpiTxnResponse(t, vpa));
    }

    // ─────────────────────────────────────────────
    //  SET PRIMARY
    // ─────────────────────────────────────────────

    @Transactional
    public void setPrimary(String vpa, String username) {
        UpiId target = findActiveVpa(vpa);
        validateOwnershipByUser(target, username);

        // Remove primary from all UPI IDs on same account
        upiIdRepository
                .findByAccountIdAndStatusNot(
                        target.getAccount().getId(), UpiId.UpiStatus.DELETED)
                .forEach(u -> {
                    if (u.getIsPrimary()) {
                        u.setIsPrimary(false);
                        upiIdRepository.save(u);
                    }
                });

        target.setIsPrimary(true);
        upiIdRepository.save(target);

        log.info("Primary UPI set: {} on account: {}",
                vpa, target.getAccount().getAccountNumber());
    }

    // ─────────────────────────────────────────────
    //  BLOCK / DELETE
    // ─────────────────────────────────────────────

    @Transactional
    public void blockUpiId(String vpa, String username) {
        UpiId u = findActiveVpa(vpa);
        validateOwnershipByUser(u, username);
        u.setStatus(UpiId.UpiStatus.BLOCKED);
        upiIdRepository.save(u);
    }

    @Transactional
    public void deleteUpiId(String vpa, String username) {
        UpiId u = findActiveVpa(vpa);
        validateOwnershipByUser(u, username);
        if (u.getIsPrimary()) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_PRIMARY",
                    "Cannot delete the primary UPI ID. Set another as primary first.");
        }
        u.setStatus(UpiId.UpiStatus.DELETED);
        upiIdRepository.save(u);
    }

    // ─────────────────────────────────────────────
    //  UPDATE LIMITS
    // ─────────────────────────────────────────────

    @Transactional
    public UpiIdResponseDTO updateLimits(
            String vpa, Long dailyLimit, Long perTxnLimit, String username) {

        UpiId u = findActiveVpa(vpa);
        validateOwnershipByUser(u, username);

        if (dailyLimit  != null && dailyLimit  > 0)
            u.setDailyLimit(Math.min(dailyLimit, 100000L));
        if (perTxnLimit != null && perTxnLimit > 0)
            u.setPerTxnLimit(Math.min(perTxnLimit, 100000L));

        upiIdRepository.save(u);
        return buildUpiResponse(u);
    }

    // ─────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private UpiId findActiveVpa(String vpa) {
        return upiIdRepository
                .findByVpaAndStatus(vpa, UpiId.UpiStatus.ACTIVE)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "VPA_NOT_FOUND",
                        "UPI ID not found or inactive: " + vpa));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private Account findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                        "Account not found: " + accountNumber));
    }

    private void validateOwnership(Account account, String username) {
        if (!account.getUser().getUsername().equals(username)) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "Access denied to this account.");
        }
    }

    private void validateOwnershipByUser(UpiId upiId, String username) {
        if (!upiId.getUser().getUsername().equals(username)) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "Access denied to this UPI ID.");
        }
    }

    private void validatePin(String pin, String confirm) {
        if (pin == null ||
            (!pin.matches("\\d{4}") && !pin.matches("\\d{6}"))) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_PIN",
                    "UPI PIN must be exactly 4 or 6 digits.");
        }
        if (!pin.equals(confirm)) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "PIN_MISMATCH",
                    "PIN and confirm PIN do not match.");
        }
    }



    // Replace notifyUpiTransaction():
    private void notifyUpiTransaction(
            UpiId sender, UpiId receiver, UpiTransaction txn) {
        try {
            String senderName = sender.getUser().getFullName() != null
                    ? sender.getUser().getFullName()
                    : sender.getUser().getUsername();
            String receiverName = receiver.getUser().getFullName() != null
                    ? receiver.getUser().getFullName()
                    : receiver.getUser().getUsername();

            // Notify sender
            mailService.sendMail(
                    sender.getUser().getEmail(),
                    "NeoBank UPI — Payment Sent",
                    """
                    Dear %s,

                    ₹%s sent successfully.

                    To     : %s (%s)
                    Via    : UPI
                    Ref    : %s
                    Time   : %s

                    NeoBank Team
                    """.formatted(
                            senderName,
                            txn.getAmount().toPlainString(),
                            receiverName,
                            txn.getReceiverVpa(),
                            txn.getUpiReference(),
                            txn.getCreatedAt().format(FMT)
                    )
            );

            // Notify receiver
            mailService.sendMail(
                    receiver.getUser().getEmail(),
                    "NeoBank UPI — Payment Received",
                    """
                    Dear %s,

                    ₹%s received.

                    From   : %s (%s)
                    Via    : UPI
                    Ref    : %s
                    Time   : %s

                    NeoBank Team
                    """.formatted(
                            receiverName,
                            txn.getAmount().toPlainString(),
                            senderName,
                            txn.getSenderVpa(),
                            txn.getUpiReference(),
                            txn.getCreatedAt().format(FMT)
                    )
            );

        } catch (Exception e) {
            log.warn("UPI email notification failed for ref: {}",
                    txn.getUpiReference());
        }
    }

    private VpaLookupResponseDTO buildLookupResponse(UpiId u, String query) {
        String name = u.getUser().getFullName() != null
                ? u.getUser().getFullName()
                : u.getUser().getUsername();
        String accNo = u.getAccount().getAccountNumber();
        String masked = "•••• " + accNo.substring(accNo.length() - 4);

        return VpaLookupResponseDTO.builder()
                .query(query)
                .resolvedVpa(u.getVpa())       // actual VPA even if searched by phone
                .accountHolderName(name)
                .maskedAccountNumber(masked)
                .valid(true)
                .message(null)
                .build();
    }

    public UpiIdResponseDTO buildUpiResponse(UpiId u) {
        return UpiIdResponseDTO.builder()
                .id(u.getId())
                .vpa(u.getVpa())
                .accountNumber(u.getAccount().getAccountNumber())
                .accountType(u.getAccount().getAccountType().name())
                .status(u.getStatus().name())
                .isPrimary(u.getIsPrimary())
                .pinSet(u.getPinSet())
                .dailyLimit(u.getDailyLimit())
                .perTxnLimit(u.getPerTxnLimit())
                .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : null)
                .lastUsedAt(u.getLastUsedAt() != null ? u.getLastUsedAt().format(FMT) : null)
                .pinLocked(u.isPinLocked())
                .build();
    }

    private UpiTransactionResponseDTO buildUpiTxnResponse(
            UpiTransaction t, String myVpa) {

        boolean isSent = t.getSenderVpa().equals(myVpa);

        return UpiTransactionResponseDTO.builder()
                .id(t.getId())
                .upiReference(t.getUpiReference())
                .senderVpa(t.getSenderVpa())
                .receiverVpa(t.getReceiverVpa())
                .amount(t.getAmount().toPlainString())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt() != null
                        ? t.getCreatedAt().format(FMT) : null)
                .type(isSent ? "SENT" : "RECEIVED")
                .build();
    }
}