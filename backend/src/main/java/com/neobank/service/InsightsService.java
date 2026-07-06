package com.neobank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.FinancialInsightsDTO;
import com.neobank.dto.TrendEntryDTO;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.enums.TransactionType;
import com.neobank.repository.InsightsRepository;
import com.neobank.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class InsightsService {

	@Autowired
	private InsightsRepository insightsRepository;

	@Autowired
	private UserRepository userRepository;

	public FinancialInsightsDTO buildInsights(Long userId) {
		// 1. Cross-user access check
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		User currentUser = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated"));

		if (!currentUser.getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-user access is prohibited");
		}

		// 2. Fetch Aggregations
		BigDecimal totalIncome = insightsRepository.getTotalIncome(userId);
		if (totalIncome == null) {
			totalIncome = BigDecimal.ZERO;
		}
		totalIncome = totalIncome.setScale(2, BigDecimal.ROUND_HALF_UP);

		BigDecimal totalExpense = insightsRepository.getTotalExpense(userId);
		if (totalExpense == null) {
			totalExpense = BigDecimal.ZERO;
		}
		totalExpense = totalExpense.setScale(2, BigDecimal.ROUND_HALF_UP);

		BigDecimal savings = totalIncome.subtract(totalExpense);

		// 3. Fetch trend for last 6 calendar months
		LocalDateTime startDate = LocalDateTime.now().minusMonths(5).withDayOfMonth(1)
				.withHour(0).withMinute(0).withSecond(0).withNano(0);
		List<Transaction> transactions = insightsRepository.findAllTransactionsForInsights(userId, startDate);

		// Group transactions in Java
		Map<String, TrendEntryDTO> groupedMap = new HashMap<>();
		for (Transaction tx : transactions) {
			LocalDateTime txDate = tx.getTransactionDate();
			if (txDate == null) {
				continue;
			}
			int year = txDate.getYear();
			int month = txDate.getMonthValue();
			String key = year + "-" + month;

			TrendEntryDTO entry = groupedMap.computeIfAbsent(key, k -> new TrendEntryDTO(year, month, BigDecimal.ZERO, BigDecimal.ZERO));
			BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;

			if (tx.getTransactionType() == TransactionType.CREDIT) {
				entry.setTotalIncome(entry.getTotalIncome().add(amount));
			} else if (tx.getTransactionType() == TransactionType.DEBIT) {
				entry.setTotalExpense(entry.getTotalExpense().add(amount));
			}
		}

		// Pad missing months
		List<TrendEntryDTO> trendSummary = new ArrayList<>();
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

		for (int i = 5; i >= 0; i--) {
			LocalDateTime targetMonth = now.minusMonths(i);
			int year = targetMonth.getYear();
			int month = targetMonth.getMonthValue();
			String monthLabel = targetMonth.format(formatter);
			String key = year + "-" + month;

			TrendEntryDTO match = groupedMap.get(key);

			if (match != null) {
				match.setMonthLabel(monthLabel);
				match.setTotalIncome(match.getTotalIncome().setScale(2, BigDecimal.ROUND_HALF_UP));
				match.setTotalExpense(match.getTotalExpense().setScale(2, BigDecimal.ROUND_HALF_UP));
				trendSummary.add(match);
			} else {
				trendSummary.add(new TrendEntryDTO(year, month, monthLabel, 
						BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)));
			}
		}

		return new FinancialInsightsDTO(totalIncome, totalExpense, savings, trendSummary);
	}
}
