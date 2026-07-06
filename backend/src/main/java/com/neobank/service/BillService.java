package com.neobank.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

import com.neobank.dto.BillRequest;
import com.neobank.dto.BillResponse;
import com.neobank.dto.BillStatusUpdateRequest;
import com.neobank.entity.Bill;
import com.neobank.entity.Reward;
import com.neobank.entity.User;
import com.neobank.enums.BillStatus;
import com.neobank.repository.BillRepository;
import com.neobank.repository.RewardRepository;
import com.neobank.repository.UserRepository;
import com.neobank.entity.Account;
import com.neobank.entity.Transaction;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.TransactionType;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BillService {

    private static final int REMINDER_THRESHOLD_DAYS = 3;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public BillResponse createBill(BillRequest request) {
        User user = getAuthenticatedUser();

        // Validate due date is in the future
        if (request.getDueDate() == null || !request.getDueDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be a future date.");
        }

        // Validate amount > 0
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill amount must be greater than zero.");
        }

        // Check duplicate biller in same month
        int year = request.getDueDate().getYear();
        int month = request.getDueDate().getMonthValue();
        if (billRepository.findDuplicate(user.getId(), request.getBillerName(), year, month).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A bill for this biller already exists for the specified month.");
        }

        Bill bill = new Bill();
        bill.setUser(user);
        bill.setBillerName(request.getBillerName());
        bill.setAmount(request.getAmount());
        bill.setDueDate(request.getDueDate());

        com.neobank.enums.BudgetCategory category = com.neobank.enums.BudgetCategory.OTHER;
        if (request.getCategory() != null) {
            try {
                category = com.neobank.enums.BudgetCategory.valueOf(request.getCategory().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category. Valid values: GROCERIES, UTILITIES, RENT, ENTERTAINMENT, OTHER");
            }
        }
        bill.setCategory(category);

        bill = billRepository.save(bill);
        return toBillResponse(bill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> getAllBills() {
        User user = getAuthenticatedUser();
        return billRepository.findAllByUserId(user.getId())
                .stream().map(this::toBillResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillResponse getBillById(Long billId) {
        User user = getAuthenticatedUser();
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found."));
        if (!bill.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access another user's bill.");
        }
        return toBillResponse(bill);
    }

    @Transactional
    public BillResponse updateBillStatus(Long billId, BillStatusUpdateRequest request) {
        User user = getAuthenticatedUser();
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found."));

        if (!bill.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot modify another user's bill.");
        }

        BillStatus newStatus;
        try {
            newStatus = BillStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status. Valid values: PENDING, PAID, OVERDUE");
        }

        // Validate transition: only PENDING -> PAID/OVERDUE or OVERDUE -> PAID
        if (bill.getStatus() != BillStatus.PENDING && bill.getStatus() != BillStatus.OVERDUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status can only be updated from PENDING or OVERDUE state.");
        }
        if (bill.getStatus() == BillStatus.PENDING) {
            if (newStatus != BillStatus.PAID && newStatus != BillStatus.OVERDUE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition. PENDING can only transition to PAID or OVERDUE.");
            }
        } else if (bill.getStatus() == BillStatus.OVERDUE) {
            if (newStatus != BillStatus.PAID) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition. OVERDUE can only transition to PAID.");
            }
        }

        if (newStatus == BillStatus.PAID) {
            if (request.getAccountId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID is required to pay the bill.");
            }

            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected account not found."));

            if (!account.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot pay a bill using another user's account.");
            }

            if (!Boolean.TRUE.equals(account.getIsActive()) || account.getAccountStatus() != AccountStatus.APPROVED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Selected account is not approved or is inactive.");
            }

            if (account.getBalance().compareTo(bill.getAmount()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance in the selected account.");
            }

            BigDecimal newBalance = account.getBalance().subtract(bill.getAmount());
            account.setBalance(newBalance);
            accountRepository.save(account);

            Transaction tx = new Transaction();
            tx.setAccount(account);
            tx.setTransactionType(TransactionType.DEBIT);
            tx.setAmount(bill.getAmount());
            tx.setBalanceAfter(newBalance);
            tx.setDescription("Bill Payment: " + bill.getBillerName() + " - " + bill.getCategory().name().toLowerCase());
            tx.setTransactionDate(LocalDateTime.now());
            transactionRepository.save(tx);
        }

        boolean paidOnTime = bill.getStatus() == BillStatus.PENDING;

        bill.setStatus(newStatus);
        bill = billRepository.save(bill);

        // Award reward points on bill payment only if paid on time
        Integer pointsEarned = null;
        if (newStatus == BillStatus.PAID) {
            if (paidOnTime) {
                pointsEarned = rewardService.awardPointsForAction(user, "BILL_PAY", 100);
            } else {
                pointsEarned = 0;
            }
        }

        return toBillResponse(bill, pointsEarned);
    }

    @Transactional
    public void deleteBill(Long billId) {
        User user = getAuthenticatedUser();
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found."));
        if (!bill.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete another user's bill.");
        }
        billRepository.delete(bill);
    }

    private BillResponse toBillResponse(Bill bill) {
        return toBillResponse(bill, null);
    }

    private BillResponse toBillResponse(Bill bill, Integer pointsEarned) {
        boolean remindMe = false;
        if (bill.getStatus() == BillStatus.PENDING && bill.getDueDate() != null) {
            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), bill.getDueDate());
            remindMe = daysUntilDue >= 0 && daysUntilDue <= REMINDER_THRESHOLD_DAYS;
        }
        return new BillResponse(
                bill.getId(),
                bill.getBillerName(),
                bill.getAmount(),
                bill.getDueDate(),
                bill.getStatus().name(),
                remindMe,
                bill.getCreatedAt(),
                bill.getCategory() != null ? bill.getCategory().name() : "OTHER",
                pointsEarned
        );
    }

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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
