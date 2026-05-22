// src/main/java/com/neobank/service/BudgetService.java

package com.neobank.service;

import com.neobank.dto.budget.*;
import com.neobank.entity.*;
import com.neobank.entity.Budget.BudgetCategory;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository     budgetRepository;
    private final UserRepository       userRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository    accountRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final int REMINDER_DAYS = 3;

    // ─────────────────────────────────────────────
    //  CREATE BUDGET
    //  BR-01: limit > 0
    //  BR-02: one per category per month per user
    //  BR-03: valid category
    //  BR-04: userId from JWT, never request body
    // ─────────────────────────────────────────────

    @Transactional
    public BudgetResponseDTO createBudget(BudgetRequestDTO req, String username) {

        User user = findUser(username);

        // Validate category
        BudgetCategory category;
        try {
            category = BudgetCategory.valueOf(req.getCategory().toUpperCase());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY",
                    "Invalid category. Valid values: " +
                    Arrays.toString(BudgetCategory.values()));
        }

        // Validate month format
        LocalDate budgetMonth = parseMonth(req.getBudgetMonth());

        // Validate amount > 0
        BigDecimal limit;
        try {
            limit = new BigDecimal(req.getLimitAmount());
        } catch (Exception e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT",
                    "Invalid limit amount.");
        }
        if (limit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT",
                    "Budget limit must be greater than zero.");
        }

        // Duplicate check
        if (budgetRepository.existsByUserIdAndCategoryAndBudgetMonth(
                user.getId(), category, budgetMonth)) {
            throw new NeoBankException(HttpStatus.CONFLICT, "BUDGET_EXISTS",
                    "A budget for " + category.name() +
                    " in " + req.getBudgetMonth() + " already exists.");
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .budgetMonth(budgetMonth)
                .limitAmount(limit)
                .build();

        Budget saved = budgetRepository.save(budget);
        log.info("Budget created: {} {} ₹{} for {}", category, budgetMonth, limit, username);

        return buildResponse(saved);
    }

    // ─────────────────────────────────────────────
    //  GET SUMMARY
    //  month format: YYYY-MM
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BudgetSummaryResponseDTO getSummary(
            Long userId, String monthStr, String username) {

        // BR-05: cross-user check
        User user = findUser(username);
        if (!user.getId().equals(userId)) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "Access denied to this budget data.");
        }

        LocalDate budgetMonth = parseMonth(monthStr);
        LocalDate monthStart  = budgetMonth.withDayOfMonth(1);
        LocalDate monthEnd    = budgetMonth.withDayOfMonth(
                budgetMonth.lengthOfMonth());

        List<Budget> budgets = budgetRepository
                .findByUserIdAndBudgetMonthOrderByCategoryAsc(
                        user.getId(), budgetMonth);

        // Compute spending per category from transactions
        Map<BudgetCategory, BigDecimal> spentMap =
                computeSpending(user.getId(), monthStart, monthEnd);

        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        List<BudgetSummaryDTO> categories = new ArrayList<>();

        for (Budget b : budgets) {
            BigDecimal spent = spentMap.getOrDefault(
                    b.getCategory(), BigDecimal.ZERO);
            BigDecimal remaining = b.getLimitAmount().subtract(spent);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO;
            }

            double utilization = spent
                    .divide(b.getLimitAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            String status = utilization >= 100 ? "EXCEEDED"
                          : utilization >= 75  ? "WARNING"
                          : "SAFE";

            categories.add(BudgetSummaryDTO.builder()
                    .category(b.getCategory().name())
                    .limitAmount(b.getLimitAmount().toPlainString())
                    .spentAmount(spent.toPlainString())
                    .remainingAmount(remaining.toPlainString())
                    .utilizationPercent(
                            Math.round(utilization * 100.0) / 100.0)
                    .status(status)
                    .build());

            totalLimit = totalLimit.add(b.getLimitAmount());
            totalSpent = totalSpent.add(spent);
        }

        double overallUtil = totalLimit.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalSpent.divide(totalLimit, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();

        return BudgetSummaryResponseDTO.builder()
                .month(monthStr)
                .totalLimit(totalLimit.toPlainString())
                .totalSpent(totalSpent.toPlainString())
                .overallUtilization(Math.round(overallUtil * 100.0) / 100.0)
                .categories(categories)
                .build();
    }

    // ─────────────────────────────────────────────
    //  GET ALL BUDGETS for user
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BudgetResponseDTO> getMyBudgets(String username) {
        User user = findUser(username);
        return budgetRepository
                .findByUserIdOrderByBudgetMonthDesc(user.getId())
                .stream().map(this::buildResponse).toList();
    }

    // ─────────────────────────────────────────────
    //  DELETE BUDGET
    // ─────────────────────────────────────────────

    @Transactional
    public void deleteBudget(Long budgetId, String username) {
        User user = findUser(username);
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "BUDGET_NOT_FOUND",
                        "Budget not found."));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "Access denied.");
        }
        budgetRepository.delete(budget);
    }

    // ─────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private LocalDate parseMonth(String monthStr) {
        try {
            YearMonth ym = YearMonth.parse(monthStr);
            return ym.atDay(1);
        } catch (DateTimeParseException e) {
            throw new NeoBankException(HttpStatus.BAD_REQUEST, "INVALID_MONTH",
                    "Month must be in YYYY-MM format (e.g. 2026-04).");
        }
    }

    // Map transaction descriptions to budget categories
    private Map<BudgetCategory, BigDecimal> computeSpending(
            Long userId, LocalDate start, LocalDate end) {

        // Get all accounts of user
        List<Account> accounts = accountRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        Map<BudgetCategory, BigDecimal> map = new EnumMap<>(BudgetCategory.class);
        for (BudgetCategory cat : BudgetCategory.values()) {
            map.put(cat, BigDecimal.ZERO);
        }

        for (Account account : accounts) {
            // Get outgoing transactions (debit) in the month
            transactionRepository
                    .findDebitsByAccountAndDateRange(
                            account.getId(), start.atStartOfDay(),
                            end.plusDays(1).atStartOfDay())
                    .forEach(txn -> {
                        BudgetCategory cat = mapCategory(txn);
                        map.merge(cat, txn.getAmount(), BigDecimal::add);
                    });
        }
        return map;
    }

    // Keyword mapping for category detection

    
    // private BudgetCategory mapCategory(Transaction txn) {
    //     String desc = txn.getDescription() != null
    //             ? txn.getDescription().toLowerCase() : "";


    //     if (desc.contains("grocery") || desc.contains("groceries")
    //      || desc.contains("supermarket") || desc.contains("food")
    //      || desc.contains("vegetables") || desc.contains("fruits")) {
    //         return BudgetCategory.GROCERIES;
    //     }
    //     if (desc.contains("electricity") || desc.contains("water")
    //      || desc.contains("gas") || desc.contains("utility")
    //      || desc.contains("internet") || desc.contains("broadband")
    //      || desc.contains("phone") || desc.contains("mobile bill")) {
    //         return BudgetCategory.UTILITIES;
    //     }
    //     if (desc.contains("rent") || desc.contains("house")
    //      || desc.contains("apartment") || desc.contains("flat")) {
    //         return BudgetCategory.RENT;
    //     }
    //     if (desc.contains("movie") || desc.contains("netflix")
    //      || desc.contains("entertainment") || desc.contains("restaurant")
    //      || desc.contains("dining") || desc.contains("hotel")
    //      || desc.contains("travel") || desc.contains("shopping")) {
    //         return BudgetCategory.ENTERTAINMENT;
    //     }
    //     if (txn.getTransactionType() == Transaction.TransactionType.TRANSFER
    //      || txn.getTransactionType() == Transaction.TransactionType.NEFT
    //      || txn.getTransactionType() == Transaction.TransactionType.UPI) {
    //         return BudgetCategory.TRANSFER;
    //     }

    //     return BudgetCategory.OTHER;
    // }

    private static final Map<BudgetCategory, String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>() {{
    put(BudgetCategory.TRANSFER, new String[]{
        "transfer", "neft", "upi", "imps", "rtgs"
    });
    put(BudgetCategory.GROCERIES, new String[]{
        "grocery", "groceries", "supermarket", "hypermarket",
        "vegetable", "vegetables", "fruit", "fruits",
        "food", "kirana", "provisions"
    });
    put(BudgetCategory.UTILITIES, new String[]{
        "electricity", "water bill", "utility", "utilities",
        "internet", "broadband", "wifi", "mobile", "postpaid",
        "prepaid", "recharge", "dth", "cable", "gas bill"
    });
    put(BudgetCategory.RENT, new String[]{
        "rent", "house rent", "apartment", "flat",
        "lease", "pg", "hostel", "maintenance"
    });
    put(BudgetCategory.ENTERTAINMENT, new String[]{
        "movie", "netflix", "amazon prime", "hotstar", "spotify",
        "entertainment", "restaurant", "dining", "cafe",
        "swiggy", "zomato", "hotel", "travel", "shopping",
        "mall", "flipkart", "amazon", "petrol", "diesel",
        "fuel", "hospital", "clinic", "pharmacy", "medicine",
        "doctor", "medical", "school", "college", "tuition",
        "course", "education", "fees"
    });
}};

// Max allowed edit distance for fuzzy matching
private static final int MAX_EDIT_DISTANCE = 2;

private BudgetCategory mapCategory(Transaction txn) {
    String desc = txn.getDescription() != null
            ? txn.getDescription().toLowerCase().trim() : "";



    // Split description into individual words for fuzzy matching
    String[] descWords = desc.split("\\s+");

    for (Map.Entry<BudgetCategory, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
        for (String keyword : entry.getValue()) {
            // 1. Direct substring match (fast path)
            if (desc.contains(keyword)) {
                return entry.getKey();
            }
            // 2. Fuzzy match each word in description against keyword
            for (String word : descWords) {
                if (isFuzzyMatch(word, keyword)) {
                    return entry.getKey();
                }
            }
        }
    }

        // Check transaction type first i do last
    if (txn.getTransactionType() == Transaction.TransactionType.TRANSFER
     || txn.getTransactionType() == Transaction.TransactionType.NEFT
     || txn.getTransactionType() == Transaction.TransactionType.UPI) {
        return BudgetCategory.TRANSFER;
    }

    return BudgetCategory.OTHER;
}

/**
 * Returns true if two strings are within MAX_EDIT_DISTANCE edits (Levenshtein).
 * Handles spelling mistakes like "groccery" → "grocery", "nteflix" → "netflix"
 */
private boolean isFuzzyMatch(String a, String b) {
    // Skip fuzzy match for very short words to avoid false positives
    if (a.length() < 4 || b.length() < 4) return false;
    return levenshteinDistance(a, b) <= MAX_EDIT_DISTANCE;
}

private int levenshteinDistance(String a, String b) {
    int m = a.length(), n = b.length();
    int[][] dp = new int[m + 1][n + 1];

    for (int i = 0; i <= m; i++) dp[i][0] = i;
    for (int j = 0; j <= n; j++) dp[0][j] = j;

    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                dp[i][j] = dp[i - 1][j - 1];
            } else {
                dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                                Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
    }
    return dp[m][n];
}



    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private BudgetResponseDTO buildResponse(Budget b) {
        return BudgetResponseDTO.builder()
                .id(b.getId())
                .category(b.getCategory().name())
                .budgetMonth(b.getBudgetMonth().toString().substring(0, 7))
                .limitAmount(b.getLimitAmount().toPlainString())
                .createdAt(b.getCreatedAt() != null
                        ? b.getCreatedAt().format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy")) : null)
                .build();
    }
}