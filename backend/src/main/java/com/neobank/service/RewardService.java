package com.neobank.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.neobank.dto.RedeemRequest;
import com.neobank.dto.RewardResponse;
import com.neobank.entity.Account;
import com.neobank.entity.Budget;
import com.neobank.entity.Reward;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.enums.BudgetCategory;
import com.neobank.enums.TransactionType;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.BudgetRepository;
import com.neobank.repository.RewardRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;

@Service
public class RewardService {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

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

    private void processAutoRewards(Reward reward) {
        User user = reward.getUser();
        boolean rewardUpdated = false;

        // 1. Daily Login Points (+20 pts base)
        LocalDate today = LocalDate.now();
        if (reward.getLastLoginPointsDate() == null || reward.getLastLoginPointsDate().isBefore(today)) {
            int currentPoints = reward.getPointsBalance();
            int multiplier = currentPoints >= 1000 ? 5 : (currentPoints >= 500 ? 3 : 1);
            reward.setPointsBalance(currentPoints + 20 * multiplier);
            reward.setLastLoginPointsDate(today);
            rewardUpdated = true;
        }

        // 2. Stay Under Budget Points (+200 pts monthly base)
        String currentMonthStr = today.getYear() + "-" + String.format("%02d", today.getMonthValue());
        if (reward.getLastBudgetPointsMonth() == null || !reward.getLastBudgetPointsMonth().equals(currentMonthStr)) {
            LocalDate budgetMonth = today.withDayOfMonth(1);
            List<Budget> budgets = budgetRepository.findAllByUserIdAndBudgetMonth(user.getId(), budgetMonth);
            if (budgets != null && !budgets.isEmpty()) {
                List<Account> userAccounts = accountRepository.findAllByUserId(user.getId());
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

                boolean staysUnder = true;
                for (Budget budget : budgets) {
                    BigDecimal spent = spentByCategory.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
                    if (spent.compareTo(budget.getLimitAmount()) > 0) {
                        staysUnder = false;
                        break;
                    }
                }

                if (staysUnder) {
                    int currentPoints = reward.getPointsBalance();
                    int multiplier = currentPoints >= 1000 ? 5 : (currentPoints >= 500 ? 3 : 1);
                    reward.setPointsBalance(currentPoints + 200 * multiplier);
                    reward.setLastBudgetPointsMonth(currentMonthStr);
                    rewardUpdated = true;
                }
            }
        }

        // Auto-unlock Premium Card if points reach 500
        if (reward.getPointsBalance() >= 500 && !Boolean.TRUE.equals(reward.getIsPremiumCard())) {
            reward.setIsPremiumCard(true);
            rewardUpdated = true;
        }

        if (rewardUpdated) {
            rewardRepository.save(reward);
        }
    }

    @Transactional
    public int awardPointsForAction(User user, String actionType, int basePoints) {
        Reward reward = rewardRepository.findByUserId(user.getId()).orElseGet(() -> {
            Reward r = new Reward();
            r.setUser(user);
            r.setPointsBalance(0);
            return rewardRepository.save(r);
        });

        int currentPoints = reward.getPointsBalance();
        int multiplier = 1;
        if (currentPoints >= 1000) {
            multiplier = 5; // Platinum: 5x points
        } else if (currentPoints >= 500) {
            multiplier = 3; // Gold: 3x points
        } else if (currentPoints >= 100) {
            if ("BILL_PAY".equals(actionType)) {
                multiplier = 2; // Silver: 2x bill pay points
            }
        }

        int pointsToAward = basePoints * multiplier;
        reward.setPointsBalance(currentPoints + pointsToAward);

        // Auto-unlock Premium Card if points reach 500
        if (reward.getPointsBalance() >= 500 && !Boolean.TRUE.equals(reward.getIsPremiumCard())) {
            reward.setIsPremiumCard(true);
        }

        rewardRepository.save(reward);
        return pointsToAward;
    }

    @Transactional
    public RewardResponse getRewardBalance(Long userId) {
        User authenticatedUser = getAuthenticatedUser();

        if (!authenticatedUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access another user's rewards.");
        }

        Reward reward = rewardRepository.findByUserId(userId).orElseGet(() -> {
            Reward r = new Reward();
            r.setUser(authenticatedUser);
            r.setPointsBalance(0);
            return rewardRepository.save(r);
        });

        processAutoRewards(reward);

        return new RewardResponse(reward.getId(), userId, reward.getPointsBalance(), reward.getIsPremiumCard(), reward.getLastUpdated());
    }

    @Transactional
    public void addPoints(Long userId, int points) {
        Reward reward = rewardRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            Reward r = new Reward();
            r.setUser(user);
            r.setPointsBalance(0);
            return r;
        });
        reward.setPointsBalance(reward.getPointsBalance() + points);
        
        // Auto-unlock Premium Card if points reach 500
        if (reward.getPointsBalance() >= 500 && !Boolean.TRUE.equals(reward.getIsPremiumCard())) {
            reward.setIsPremiumCard(true);
        }
        rewardRepository.save(reward);
    }

    @Transactional
    public void deductPoints(Long userId, int points) {
        Reward reward = rewardRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No reward record found."));
        if (reward.getPointsBalance() < points) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient reward points. Balance cannot go below zero.");
        }
        reward.setPointsBalance(reward.getPointsBalance() - points);
        rewardRepository.save(reward);
    }

    @Transactional
    public RewardResponse getRewardBalanceByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Reward reward = rewardRepository.findByUserId(user.getId()).orElseGet(() -> {
            Reward r = new Reward();
            r.setUser(user);
            r.setPointsBalance(0);
            return rewardRepository.save(r);
        });

        processAutoRewards(reward);

        return new RewardResponse(reward.getId(), user.getId(), reward.getPointsBalance(), reward.getIsPremiumCard(), reward.getLastUpdated());
    }

    @Transactional
    public void redeemReward(String email, RedeemRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Reward reward = rewardRepository.findByUserId(user.getId()).orElseGet(() -> {
            Reward r = new Reward();
            r.setUser(user);
            r.setPointsBalance(0);
            return rewardRepository.save(r);
        });

        if (reward.getPointsBalance() < request.getPoints()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient reward points.");
        }

        // Deduct points
        reward.setPointsBalance(reward.getPointsBalance() - request.getPoints());
        rewardRepository.save(reward);

        // Perform reward action
        if ("cashback-50".equals(request.getRewardId()) || "cashback-100".equals(request.getRewardId())) {
            if (request.getAccountId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID is required to credit cashback.");
            }
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected account not found."));

            if (!account.getUser().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot credit cashback to another user's account.");
            }

            BigDecimal cashbackAmount = "cashback-50".equals(request.getRewardId()) ? new BigDecimal("50.00") : new BigDecimal("100.00");
            account.setBalance(account.getBalance().add(cashbackAmount));
            accountRepository.save(account);

            // Create CREDIT transaction
            Transaction tx = new Transaction();
            tx.setAccount(account);
            tx.setTransactionType(TransactionType.CREDIT);
            tx.setAmount(cashbackAmount);
            tx.setBalanceAfter(account.getBalance());
            tx.setDescription("Cashback Reward Credit: " + ("cashback-50".equals(request.getRewardId()) ? "₹50" : "₹100"));
            tx.setTransactionDate(LocalDateTime.now());
            transactionRepository.save(tx);
        } else if ("premium-card".equals(request.getRewardId())) {
            reward.setIsPremiumCard(true);
            rewardRepository.save(reward);
        }
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
