package com.neobank.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.AdminDashboardDTO;
import com.neobank.dto.PendingApprovalDTO;
import com.neobank.dto.TransactionResponse;
import com.neobank.dto.UserActivityDTO;
import com.neobank.dto.AdminTransactionAnalyticsDTO;
import com.neobank.dto.AdminLoanAnalyticsDTO;
import com.neobank.entity.Account;
import com.neobank.entity.LoanApplication;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.entity.SystemAuditLog;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.LoanStatus;
import com.neobank.enums.Role;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.AdminDashboardRepository;
import com.neobank.repository.LoanApplicationRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;
import com.neobank.repository.SystemAuditLogRepository;
import com.neobank.repository.LoanRepaymentRepository;
import com.neobank.repository.LoanAccountRepository;

@Service
public class AdminService {

	private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private AdminDashboardRepository adminDashboardRepository;

	@Autowired
	private LoanApplicationRepository loanApplicationRepository;

	@Autowired
	private SystemAuditLogRepository systemAuditLogRepository;

	@Autowired
	private LoanRepaymentRepository loanRepaymentRepository;

	@Autowired
	private LoanAccountRepository loanAccountRepository;

	@Autowired
	private AuthService authService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	// =========================
	// DASHBOARD
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public AdminDashboardDTO getDashboardStats() {
		long totalUsers = adminDashboardRepository.countAllUsers();
		long totalActiveUsers = adminDashboardRepository.countAllActiveUsers();
		long totalLoans = adminDashboardRepository.countAllLoans();

		long pendingLoans = adminDashboardRepository.countPendingLoans();
		long pendingAccounts = adminDashboardRepository.countPendingAccounts();
		long pendingApprovals = pendingLoans + pendingAccounts;

		long totalTransactions = adminDashboardRepository.countAllTransactions();

		BigDecimal totalIncome = adminDashboardRepository.getTotalIncomePlatform();
		BigDecimal totalExpense = adminDashboardRepository.getTotalExpensePlatform();
		BigDecimal platformSavingsRate = BigDecimal.ZERO;

		if (totalIncome != null && totalIncome.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal incomeDiff = totalIncome.subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO);
			platformSavingsRate = incomeDiff.multiply(new BigDecimal(100))
					.divide(totalIncome, 2, RoundingMode.HALF_UP);
		}

		return new AdminDashboardDTO(totalUsers, totalActiveUsers, totalLoans, pendingApprovals, totalTransactions, platformSavingsRate);
	}

	// =========================
	// SYSTEM HEALTH MONITORING
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public Map<String, Object> getSystemHealth() {
		String dbStatus = "UP";
		try {
			jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		} catch (Exception e) {
			dbStatus = "DOWN";
		}

		long serverUptimeSeconds = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
		
		// Stateless active session count: active users who made a request in the last 15 minutes
		LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(15);
		long activeUsers = systemAuditLogRepository.countActiveUsers(timeLimit);

		Map<String, Object> health = new HashMap<>();
		health.put("dbStatus", dbStatus);
		health.put("activeSessions", (int) activeUsers);
		health.put("serverUptimeSeconds", serverUptimeSeconds);

		return health;
	}

	// =========================
	// PENDING APPROVALS
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public List<PendingApprovalDTO> getPendingApprovals(String module) {
		List<PendingApprovalDTO> approvals = new ArrayList<>();

		boolean fetchLoans = (module == null || "LOAN".equalsIgnoreCase(module));
		boolean fetchAccounts = (module == null || "ACCOUNT".equalsIgnoreCase(module));

		if (fetchLoans) {
			List<LoanApplication> pendingLoans = loanApplicationRepository.findAllByStatus(LoanStatus.PENDING);
			for (LoanApplication loan : pendingLoans) {
				approvals.add(new PendingApprovalDTO(
						loan.getId(),
						"LOAN_APPLICATION",
						loan.getUser() != null ? loan.getUser().getFullName() : "Unknown",
						loan.getLoanProduct() != null ? loan.getLoanProduct().getProductName() : "Unknown Loan Product",
						loan.getRequestedAmount(),
						loan.getAppliedAt()
				));
			}
		}

		if (fetchAccounts) {
			List<Account> pendingAccounts = accountRepository.findByAccountStatus(AccountStatus.PENDING_APPROVAL);
			for (Account acc : pendingAccounts) {
				PendingApprovalDTO dto = new PendingApprovalDTO(
						acc.getId(),
						"ACCOUNT_APPROVAL",
						acc.getUser() != null ? acc.getUser().getFullName() : "Unknown",
						acc.getAccountType() != null ? acc.getAccountType().name() : "SAVINGS",
						acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO,
						acc.getCreatedAt()
				);
				// ✅ Attach KYC details so admin can verify identity before approving
				if (acc.getUser() != null) {
					dto.setApplicantEmail(acc.getUser().getEmail());
					dto.setAadhaarNumber(acc.getUser().getAadhaarNumber());
					dto.setPanNumber(acc.getUser().getPanNumber());
				}
				approvals.add(dto);
			}
		}

		// Order oldest-first
		approvals.sort((a1, a2) -> {
			if (a1.getAppliedAt() == null && a2.getAppliedAt() == null) return 0;
			if (a1.getAppliedAt() == null) return 1;
			if (a2.getAppliedAt() == null) return -1;
			return a1.getAppliedAt().compareTo(a2.getAppliedAt());
		});

		return approvals;
	}

	// =========================
	// USERS
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public List<Map<String, Object>> getAllUsers() {
		return userRepository.findAll().stream().map(this::toUserResponse).toList();
	}

	@PreAuthorize("hasRole('ADMIN')")
	public Page<Map<String, Object>> getAllUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(this::toUserResponse);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public void updateUserStatus(Long userId, boolean isActive, String actingAdminEmail) {
		User admin = userRepository.findByEmail(actingAdminEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acting admin user not found"));

		if (admin.getId().equals(userId) && !isActive) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An admin cannot deactivate their own account");
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		user.setIsActive(isActive);
		userRepository.save(user);

		// Audit logging
		logger.info("[AUDIT LOG] actingAdminId: {}, action: {}, targetResourceType: {}, targetResourceId: {}, targetUserId: {}, timestamp: {}",
				admin.getId(), "UPDATE_USER_STATUS_" + (isActive ? "ACTIVATE" : "DEACTIVATE"), "USER", userId, userId, LocalDateTime.now());
	}

	// =========================
	// USER ACTIVITY
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public UserActivityDTO getUserActivity(Long userId) {
		Pageable pageable = PageRequest.of(0, 20);
		List<TransactionResponse> recentTxs = transactionRepository.findRecentTransactionsByUserId(userId, pageable)
				.stream().map(this::toTransactionResponse).toList();

		List<LocalDateTime> loginEvents = authService.getLoginEvents(userId);

		return new UserActivityDTO(recentTxs, loginEvents);
	}

	// =========================
	// ACCOUNTS (ALL)
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public List<Map<String, Object>> getAllAccounts() {
		return accountRepository.findAll().stream().map(this::toAccountResponse).toList();
	}

	// =========================
	// ACCOUNT APPROVAL
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public List<Map<String, Object>> getPendingAccounts() {
		return accountRepository.findByAccountStatus(AccountStatus.PENDING_APPROVAL).stream()
				.map(this::toAccountResponse).toList();
	}

	/**
	 * ✅ Fetch a single account with full user KYC details so admin can review
	 * Aadhaar, PAN, email etc. before approving or rejecting the application.
	 */
	@PreAuthorize("hasRole('ADMIN')")
	public Map<String, Object> getAccountDetail(Long accountId) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
		return toAccountResponse(account);
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public void approveAccount(Long accountId) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		if (account.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not pending approval");
		}

		account.setAccountStatus(AccountStatus.APPROVED);
		account.setIsActive(true);
		accountRepository.save(account);

		// Audit logging
		String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acting admin not found"));

		logger.info("[AUDIT LOG] actingAdminId: {}, action: {}, targetResourceType: {}, targetResourceId: {}, targetUserId: {}, timestamp: {}",
				admin.getId(), "APPROVE_ACCOUNT", "ACCOUNT", accountId, account.getUser() != null ? account.getUser().getId() : null, LocalDateTime.now());
	}

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public void rejectAccount(Long accountId) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		if (account.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not pending approval");
		}

		account.setAccountStatus(AccountStatus.REJECTED);
		account.setIsActive(false);
		accountRepository.save(account);

		// Audit logging
		String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		User admin = userRepository.findByEmail(adminEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acting admin not found"));

		logger.info("[AUDIT LOG] actingAdminId: {}, action: {}, targetResourceType: {}, targetResourceId: {}, targetUserId: {}, timestamp: {}",
				admin.getId(), "REJECT_ACCOUNT", "ACCOUNT", accountId, account.getUser() != null ? account.getUser().getId() : null, LocalDateTime.now());
	}

	// =========================
	// TRANSACTIONS
	// =========================

	@PreAuthorize("hasRole('ADMIN')")
	public List<TransactionResponse> getRecentTransactions(int size) {
	    return transactionRepository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "transactionDate")))
	            .getContent().stream().map(this::toTransactionResponse).toList();
	}

	// =========================
	// ADMIN CREDIT TO USER ACCOUNT
	// =========================

	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public TransactionResponse adminCreditAccount(Long accountId, java.math.BigDecimal amount, String description) {
	    if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
	        throw new org.springframework.web.server.ResponseStatusException(
	                org.springframework.http.HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
	    }

	    com.neobank.entity.Account account = accountRepository.findByIdForUpdate(accountId)
	            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
	                    org.springframework.http.HttpStatus.NOT_FOUND, "Account not found"));

	    if (account.getAccountStatus() != com.neobank.enums.AccountStatus.APPROVED
	            || !Boolean.TRUE.equals(account.getIsActive())) {
	        throw new org.springframework.web.server.ResponseStatusException(
	                org.springframework.http.HttpStatus.FORBIDDEN,
	                "Cannot credit an account that is not approved and active");
	    }

	    java.math.BigDecimal updatedBalance = account.getBalance().add(amount);
	    account.setBalance(updatedBalance);
	    accountRepository.save(account);

	    com.neobank.entity.Transaction transaction = new com.neobank.entity.Transaction();
	    transaction.setAccount(account);
	    transaction.setTransactionType(com.neobank.enums.TransactionType.CREDIT);
	    transaction.setAmount(amount);
	    transaction.setBalanceAfter(updatedBalance);
	    transaction.setDescription(description != null ? description : "Admin credit");
	    com.neobank.entity.Transaction saved = transactionRepository.save(transaction);

	    return toTransactionResponse(saved);
	}

	// =========================
	// SPRINT 5 ANALYTICS & LOGS
	// =========================

	private LocalDateTime getStartDateForTimeframe(String timeframe) {
		LocalDateTime now = LocalDateTime.now();
		if ("7d".equals(timeframe)) {
			return now.minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
		} else if ("30d".equals(timeframe)) {
			return now.minusDays(29).withHour(0).withMinute(0).withSecond(0).withNano(0);
		} else if ("YTD".equals(timeframe)) {
			return now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timeframe");
		}
	}

	@PreAuthorize("hasRole('ADMIN')")
	public AdminTransactionAnalyticsDTO getTransactionsAnalytics(String timeframe) {
		if (!"7d".equals(timeframe) && !"30d".equals(timeframe) && !"YTD".equals(timeframe)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timeframe. Allowed values: 7d, 30d, YTD");
		}

		LocalDateTime startDate = getStartDateForTimeframe(timeframe);
		List<Transaction> txs = transactionRepository.findByTransactionDateGreaterThanEqual(startDate);

		BigDecimal totalInflow = BigDecimal.ZERO;
		BigDecimal totalOutflow = BigDecimal.ZERO;
		BigDecimal totalAmount = BigDecimal.ZERO;
		long count = 0;

		Map<String, BigDecimal> dailyVolumesMap = new LinkedHashMap<>();

		java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

		LocalDateTime temp = startDate;
		LocalDateTime now = LocalDateTime.now();
		while (!temp.isAfter(now)) {
			dailyVolumesMap.put(temp.format(dtf), BigDecimal.ZERO);
			temp = temp.plusDays(1);
		}

		for (Transaction tx : txs) {
			BigDecimal amount = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
			totalAmount = totalAmount.add(amount);
			count++;

			if (tx.getTransactionType() == com.neobank.enums.TransactionType.CREDIT) {
				totalInflow = totalInflow.add(amount);
			} else if (tx.getTransactionType() == com.neobank.enums.TransactionType.DEBIT) {
				totalOutflow = totalOutflow.add(amount);
			} else if (tx.getTransactionType() == com.neobank.enums.TransactionType.TRANSFER) {
				if (tx.getDescription() != null) {
					if (tx.getDescription().contains("Transfer from")) {
						totalInflow = totalInflow.add(amount);
					} else if (tx.getDescription().contains("Transfer to")) {
						totalOutflow = totalOutflow.add(amount);
					}
				}
			}

			String dateStr = tx.getTransactionDate().format(dtf);
			if (dailyVolumesMap.containsKey(dateStr)) {
				dailyVolumesMap.put(dateStr, dailyVolumesMap.get(dateStr).add(amount));
			} else {
				dailyVolumesMap.put(dateStr, amount);
			}
		}

		BigDecimal averageTicketSize = count > 0 
				? totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) 
				: BigDecimal.ZERO.setScale(2);

		List<Map<String, Object>> dailyVolumes = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> entry : dailyVolumesMap.entrySet()) {
			dailyVolumes.add(Map.of("date", entry.getKey(), "volume", entry.getValue().setScale(2, RoundingMode.HALF_UP)));
		}

		return new AdminTransactionAnalyticsDTO(dailyVolumes, averageTicketSize.setScale(2, RoundingMode.HALF_UP), totalInflow.setScale(2, RoundingMode.HALF_UP), totalOutflow.setScale(2, RoundingMode.HALF_UP));
	}

	@PreAuthorize("hasRole('ADMIN')")
	public AdminLoanAnalyticsDTO getLoansAnalytics(String timeframe) {
		if (!"7d".equals(timeframe) && !"30d".equals(timeframe) && !"YTD".equals(timeframe)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timeframe. Allowed values: 7d, 30d, YTD");
		}

		LocalDateTime startDate = getStartDateForTimeframe(timeframe);
		List<LoanApplication> apps = loanApplicationRepository.findByAppliedAtGreaterThanEqual(startDate);

		Map<String, Map<String, Long>> distribution = new HashMap<>();
		for (com.neobank.enums.LoanStatus status : com.neobank.enums.LoanStatus.values()) {
			distribution.put(status.name(), new HashMap<>());
		}

		for (LoanApplication app : apps) {
			String status = app.getStatus().name();
			String product = app.getLoanProduct() != null ? app.getLoanProduct().getProductName() : "Unknown";

			Map<String, Long> productMap = distribution.get(status);
			productMap.put(product, productMap.getOrDefault(product, 0L) + 1);
		}

		long npaCount = loanRepaymentRepository.countOverdueLoanAccounts();
		long totalLoans = loanAccountRepository.count();
		double npaRatio = totalLoans > 0 ? ((double) npaCount / totalLoans) * 100.0 : 0.0;
		npaRatio = Math.round(npaRatio * 100.0) / 100.0;

		return new AdminLoanAnalyticsDTO(distribution, npaCount, npaRatio);
	}

	@PreAuthorize("hasRole('ADMIN')")
	public Page<SystemAuditLog> getSystemLogs(LocalDateTime from, LocalDateTime to, Integer status, Pageable pageable) {
		return systemAuditLogRepository.findFilteredLogs(status, from, to, pageable);
	}

	// =========================
	// MAPPERS
	// =========================

	private Map<String, Object> toUserResponse(User user) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", user.getId());
		map.put("email", user.getEmail());
		map.put("fullName", user.getFullName());
		map.put("role", user.getRole());
		map.put("isActive", user.getIsActive());
		map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
		return map;
	}

	private Map<String, Object> toAccountResponse(Account account) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", account.getId());
		map.put("accountNumber", account.getAccountNumber());
		map.put("accountType", account.getAccountType());
		map.put("balance", account.getBalance());
		map.put("accountStatus", account.getAccountStatus());
		map.put("isActive", account.getIsActive());
		map.put("createdAt", account.getCreatedAt() != null ? account.getCreatedAt().toString() : null);

		if (account.getUser() != null) {
			map.put("userId", account.getUser().getId());
			map.put("userFullName", account.getUser().getFullName());
			map.put("userEmail", account.getUser().getEmail());
			// ✅ KYC credentials for admin review during approval
			map.put("aadhaarNumber", account.getUser().getAadhaarNumber());
			map.put("panNumber", account.getUser().getPanNumber());
		}

		return map;
	}

	private TransactionResponse toTransactionResponse(Transaction transaction) {
		return new TransactionResponse(transaction.getId(), transaction.getAccount().getId(),
				transaction.getAccount().getAccountNumber(), transaction.getTransactionType(), transaction.getAmount(),
				transaction.getBalanceAfter(), transaction.getDescription(), transaction.getTransactionDate());
	}
}