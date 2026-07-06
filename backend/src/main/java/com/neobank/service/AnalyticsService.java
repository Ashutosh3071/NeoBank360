package com.neobank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.*;
import com.neobank.entity.*;
import com.neobank.enums.BudgetCategory;
import com.neobank.enums.RepaymentStatus;
import com.neobank.enums.TransactionType;
import com.neobank.repository.*;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private LoanAccountRepository loanAccountRepository;

	@Autowired
	private LoanRepaymentRepository loanRepaymentRepository;

	@Autowired
	private RewardRepository rewardRepository;

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

	private void validateUser(Long userId) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		User currentUser = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

		if (!currentUser.getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-user access is prohibited");
		}
	}

	public List<CategorySpendingDTO> getSpendingAnalytics(Long userId, int months) {
		validateUser(userId);

		if (months <= 0 || months > 12) {
			months = 6;
		}

		List<CategorySpendingDTO> result = new ArrayList<>();
		List<Account> accounts = accountRepository.findAllByUserId(userId);
		YearMonth currentYM = YearMonth.now();

		for (int i = months - 1; i >= 0; i--) {
			YearMonth targetYM = currentYM.minusMonths(i);
			LocalDateTime start = targetYM.atDay(1).atStartOfDay();
			LocalDateTime end = targetYM.plusMonths(1).atDay(1).atStartOfDay();

			Map<String, BigDecimal> spendingMap = new HashMap<>();
			for (BudgetCategory cat : BudgetCategory.values()) {
				spendingMap.put(cat.name(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
			}

			for (Account acc : accounts) {
				List<Transaction> txs = transactionRepository.findByAccountIdAndTransactionDateBetween(acc.getId(), start, end);
				for (Transaction tx : txs) {
					if (tx.getTransactionType() == TransactionType.DEBIT) {
						BudgetCategory cat = mapTransactionToCategory(tx.getDescription());
						spendingMap.put(cat.name(), spendingMap.get(cat.name()).add(tx.getAmount()));
					}
				}
			}

			result.add(new CategorySpendingDTO(targetYM.toString(), spendingMap));
		}

		return result;
	}

	public WealthAnalyticsDTO getWealthAnalytics(Long userId) {
		validateUser(userId);

		List<NetWorthEntryDTO> netWorthTimeline = new ArrayList<>();
		List<Account> accounts = accountRepository.findAllByUserId(userId);
		List<LoanAccount> loanAccounts = loanAccountRepository.findAllByUserId(userId);
		YearMonth currentYM = YearMonth.now();

		for (int i = 5; i >= 0; i--) {
			YearMonth targetYM = currentYM.minusMonths(i);
			LocalDateTime monthEnd = targetYM.plusMonths(1).atDay(1).atStartOfDay().minusSeconds(1);

			BigDecimal totalBalance = BigDecimal.ZERO;
			for (Account acc : accounts) {
				if (acc.getCreatedAt().isAfter(monthEnd)) {
					continue;
				}
				BigDecimal currentBal = acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO;
				List<Transaction> txsAfter = transactionRepository.findByAccountIdAndTransactionDateBetween(acc.getId(), monthEnd.plusSeconds(1), LocalDateTime.now());
				for (Transaction tx : txsAfter) {
					boolean isCredit = tx.getTransactionType() == TransactionType.CREDIT || 
									  (tx.getTransactionType() == TransactionType.TRANSFER && tx.getDescription() != null && tx.getDescription().contains("Transfer from"));
					if (isCredit) {
						currentBal = currentBal.subtract(tx.getAmount());
					} else {
						currentBal = currentBal.add(tx.getAmount());
					}
				}
				totalBalance = totalBalance.add(currentBal);
			}

			BigDecimal totalOutstandingPrincipal = BigDecimal.ZERO;
			for (LoanAccount la : loanAccounts) {
				if (la.getDisbursedAt().isAfter(monthEnd)) {
					continue;
				}
				BigDecimal principal = la.getPrincipalAmount() != null ? la.getPrincipalAmount() : BigDecimal.ZERO;
				List<LoanRepayment> repayments = loanRepaymentRepository.findAllByLoanAccountId(la.getId());
				BigDecimal paidPrincipal = BigDecimal.ZERO;
				for (LoanRepayment rep : repayments) {
					if (rep.getPaymentStatus() == RepaymentStatus.PAID && rep.getPaidAt() != null && !rep.getPaidAt().isAfter(monthEnd)) {
						paidPrincipal = paidPrincipal.add(rep.getPrincipalComponent() != null ? rep.getPrincipalComponent() : BigDecimal.ZERO);
					}
				}
				totalOutstandingPrincipal = totalOutstandingPrincipal.add(principal.subtract(paidPrincipal));
			}

			BigDecimal netWorth = totalBalance.subtract(totalOutstandingPrincipal);
			netWorthTimeline.add(new NetWorthEntryDTO(
				targetYM.toString(),
				totalBalance.setScale(2, RoundingMode.HALF_UP),
				totalOutstandingPrincipal.setScale(2, RoundingMode.HALF_UP),
				netWorth.setScale(2, RoundingMode.HALF_UP)
			));
		}

		List<LoanPayoffForecastDTO> forecasts = new ArrayList<>();
		for (LoanAccount la : loanAccounts) {
			List<LoanRepayment> repayments = loanRepaymentRepository.findAllByLoanAccountId(la.getId());
			
			int monthsRemaining = 0;
			LocalDate payoffDate = null;
			BigDecimal outstandingPrincipal = la.getPrincipalAmount() != null ? la.getPrincipalAmount() : BigDecimal.ZERO;

			for (LoanRepayment rep : repayments) {
				if (rep.getPaymentStatus() != RepaymentStatus.PAID) {
					monthsRemaining++;
					if (payoffDate == null || rep.getDueDate().isAfter(payoffDate)) {
						payoffDate = rep.getDueDate();
					}
				} else {
					outstandingPrincipal = outstandingPrincipal.subtract(rep.getPrincipalComponent() != null ? rep.getPrincipalComponent() : BigDecimal.ZERO);
				}
			}

			if (payoffDate == null && !repayments.isEmpty()) {
				payoffDate = repayments.get(repayments.size() - 1).getDueDate();
			}

			forecasts.add(new LoanPayoffForecastDTO(
				la.getId(),
				la.getLoanApplication() != null && la.getLoanApplication().getLoanProduct() != null 
						? la.getLoanApplication().getLoanProduct().getProductName() : "Active Loan",
				monthsRemaining,
				payoffDate,
				outstandingPrincipal.setScale(2, RoundingMode.HALF_UP),
				la.getEmiAmount() != null ? la.getEmiAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO
			));
		}

		List<RewardAccrualEntryDTO> rewardAccrualHistory = new ArrayList<>();
		int cumulativePoints = 0;

		for (int i = 11; i >= 0; i--) {
			YearMonth targetYM = currentYM.minusMonths(i);
			LocalDateTime mStart = targetYM.atDay(1).atStartOfDay();
			LocalDateTime mEnd = targetYM.plusMonths(1).atDay(1).atStartOfDay();

			int pointsEarnedThisMonth = 0;
			for (Account acc : accounts) {
				List<Transaction> txs = transactionRepository.findByAccountIdAndTransactionDateBetween(acc.getId(), mStart, mEnd);
				for (Transaction tx : txs) {
					if (tx.getTransactionType() == TransactionType.DEBIT && tx.getDescription() != null && tx.getDescription().contains("Bill Payment")) {
						pointsEarnedThisMonth += 10;
					}
				}
			}

			cumulativePoints += pointsEarnedThisMonth;
			rewardAccrualHistory.add(new RewardAccrualEntryDTO(targetYM.toString(), cumulativePoints));
		}

		int currentPoints = rewardRepository.findByUserId(userId).map(Reward::getPointsBalance).orElse(0);
		if (!rewardAccrualHistory.isEmpty()) {
			int diff = currentPoints - rewardAccrualHistory.get(rewardAccrualHistory.size() - 1).getPoints();
			if (diff != 0) {
				for (RewardAccrualEntryDTO entry : rewardAccrualHistory) {
					entry.setPoints(Math.max(0, entry.getPoints() + diff));
				}
			}
		}

		return new WealthAnalyticsDTO(netWorthTimeline, forecasts, rewardAccrualHistory);
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
}
