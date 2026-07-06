package com.neobank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.BudgetRequest;
import com.neobank.dto.BudgetResponse;
import com.neobank.entity.Account;
import com.neobank.entity.Budget;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.enums.BudgetCategory;
import com.neobank.enums.TransactionType;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.BudgetRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Category keyword mapping for transaction descriptions
    private static final Map<String, BudgetCategory> CATEGORY_KEYWORDS = new HashMap<>();

    static {
        CATEGORY_KEYWORDS.put("grocery", BudgetCategory.GROCERIES);
        CATEGORY_KEYWORDS.put("groceries", BudgetCategory.GROCERIES);
        CATEGORY_KEYWORDS.put("supermarket", BudgetCategory.GROCERIES);
        CATEGORY_KEYWORDS.put("food", BudgetCategory.GROCERIES);
        CATEGORY_KEYWORDS.put("utility", BudgetCategory.UTILITIES);
        CATEGORY_KEYWORDS.put("utilities", BudgetCategory.UTILITIES);
        CATEGORY_KEYWORDS.put("electricity", BudgetCategory.UTILITIES);
        CATEGORY_KEYWORDS.put("water", BudgetCategory.UTILITIES);
        CATEGORY_KEYWORDS.put("gas", BudgetCategory.UTILITIES);
        CATEGORY_KEYWORDS.put("internet", BudgetCategory.UTILITIES);
        CATEGORY_KEYWORDS.put("rent", BudgetCategory.RENT);
        CATEGORY_KEYWORDS.put("housing", BudgetCategory.RENT);
        CATEGORY_KEYWORDS.put("entertainment", BudgetCategory.ENTERTAINMENT);
        CATEGORY_KEYWORDS.put("movie", BudgetCategory.ENTERTAINMENT);
        CATEGORY_KEYWORDS.put("gaming", BudgetCategory.ENTERTAINMENT);
        CATEGORY_KEYWORDS.put("transfer", BudgetCategory.TRANSFER);
    }

    @Transactional
    public BudgetResponse createBudget(BudgetRequest request) {
        User user = getAuthenticatedUser();

        // Validate limit > 0
        if (request.getLimitAmount() == null || request.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget limit amount must be greater than zero.");
        }

        // Validate category
        BudgetCategory category;
        try {
            category = BudgetCategory.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget category. Valid categories: GROCERIES, UTILITIES, RENT, ENTERTAINMENT, TRANSFER, OTHER");
        }

        // Validate month format YYYY-MM
        LocalDate budgetMonth = parseMonth(request.getBudgetMonth());

        // Check duplicate
        if (budgetRepository.findByUserIdAndCategoryAndBudgetMonth(user.getId(), category, budgetMonth).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A budget for this category already exists for the specified month.");
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setBudgetMonth(budgetMonth);
        budget.setLimitAmount(request.getLimitAmount());

        budget = budgetRepository.save(budget);

        return toBudgetResponse(budget, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetSummary(Long userId, String month) {
        User authenticatedUser = getAuthenticatedUser();

        // Cross-user access check
        if (!authenticatedUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access another user's budget data.");
        }

        LocalDate budgetMonth = parseMonth(month);
        List<Budget> budgets = budgetRepository.findAllByUserIdAndBudgetMonth(userId, budgetMonth);

        // Get all user accounts
        List<Account> userAccounts = accountRepository.findAllByUserId(userId);

        // Get all transactions for the month across all accounts
        LocalDateTime monthStart = budgetMonth.atStartOfDay();
        LocalDateTime monthEnd = budgetMonth.plusMonths(1).atStartOfDay();

        Map<BudgetCategory, BigDecimal> spentByCategory = new HashMap<>();
        for (Account account : userAccounts) {
            List<Transaction> transactions = transactionRepository
                    .findByAccountIdAndTransactionDateBetween(account.getId(), monthStart, monthEnd);
            for (Transaction tx : transactions) {
                if (tx.getTransactionType() == TransactionType.DEBIT) {
                    BudgetCategory cat = mapTransactionToCategory(tx.getDescription());
                    spentByCategory.merge(cat, tx.getAmount(), BigDecimal::add);
                }
            }
        }

        List<BudgetResponse> responses = new ArrayList<>();
        for (Budget budget : budgets) {
            BigDecimal spent = spentByCategory.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
            responses.add(toBudgetResponse(budget, spent));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets() {
        User user = getAuthenticatedUser();
        List<Budget> budgets = budgetRepository.findAllByUserId(user.getId());
        List<BudgetResponse> responses = new ArrayList<>();
        for (Budget budget : budgets) {
            responses.add(toBudgetResponse(budget, BigDecimal.ZERO));
        }
        return responses;
    }

    @Transactional
    public void deleteBudget(Long budgetId) {
        User user = getAuthenticatedUser();
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found."));
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete another user's budget.");
        }
        budgetRepository.delete(budget);
    }

    private BudgetCategory mapTransactionToCategory(String description) {
        if (description == null || description.isBlank()) {
            return BudgetCategory.OTHER;
        }
        String lower = description.toLowerCase();
        for (Map.Entry<String, BudgetCategory> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return BudgetCategory.OTHER;
    }

    private LocalDate parseMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be in YYYY-MM format.");
        }
        try {
            YearMonth ym = YearMonth.parse(month);
            return ym.atDay(1);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month value.");
        }
    }

    private BudgetResponse toBudgetResponse(Budget budget, BigDecimal spent) {
        BigDecimal limit = budget.getLimitAmount();
        BigDecimal remaining = limit.subtract(spent);
        double utilization = limit.compareTo(BigDecimal.ZERO) > 0
                ? spent.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().name(),
                budget.getBudgetMonth().toString().substring(0, 7),
                limit,
                spent,
                remaining,
                utilization
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
