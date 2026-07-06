package com.neobank.insights;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.neobank.entity.Account;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.AccountType;
import com.neobank.enums.Role;
import com.neobank.enums.TransactionType;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;
import com.neobank.security.JwtUtil;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class InsightsTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private JwtUtil jwtUtil;

	private User user1;
	private User user2;
	private Account activeAccount;
	private Account inactiveAccount;
	private String token1;
	private String token2;

	@BeforeEach
	void setUp() {
		// Create User 1
		user1 = new User();
		user1.setEmail("user1_" + UUID.randomUUID() + "@neobank.in");
		user1.setPasswordHash("encoded_pw");
		user1.setFullName("User One");
		user1.setRole(Role.CUSTOMER);
		user1.setAadhaarNumber("11" + (long)(Math.random()*9000000000L));
		user1.setPanNumber("ABCDE" + (int)(Math.random()*9000) + "A");
		user1.setIsActive(true);
		user1 = userRepository.saveAndFlush(user1);

		// Create User 2 (For cross-user checks)
		user2 = new User();
		user2.setEmail("user2_" + UUID.randomUUID() + "@neobank.in");
		user2.setPasswordHash("encoded_pw");
		user2.setFullName("User Two");
		user2.setRole(Role.CUSTOMER);
		user2.setAadhaarNumber("12" + (long)(Math.random()*9000000000L));
		user2.setPanNumber("FGHIJ" + (int)(Math.random()*9000) + "B");
		user2.setIsActive(true);
		user2 = userRepository.saveAndFlush(user2);

		// Generate Tokens
		token1 = jwtUtil.generateToken(user1.getId(), user1.getEmail(), user1.getRole().name());
		token2 = jwtUtil.generateToken(user2.getId(), user2.getEmail(), user2.getRole().name());

		// Create Active Account for User 1
		activeAccount = new Account();
		activeAccount.setUser(user1);
		activeAccount.setAccountNumber("ACC_" + UUID.randomUUID().toString().substring(0, 10));
		activeAccount.setAccountType(AccountType.SAVINGS);
		activeAccount.setBalance(new BigDecimal("1000.00"));
		// Initial save triggers @PrePersist
		activeAccount = accountRepository.saveAndFlush(activeAccount);
		// Subsequent save updates fields bypasses @PrePersist
		activeAccount.setAccountStatus(AccountStatus.APPROVED);
		activeAccount.setIsActive(true);
		activeAccount = accountRepository.saveAndFlush(activeAccount);

		// Create Inactive Account for User 1
		inactiveAccount = new Account();
		inactiveAccount.setUser(user1);
		inactiveAccount.setAccountNumber("ACC_" + UUID.randomUUID().toString().substring(0, 10));
		inactiveAccount.setAccountType(AccountType.CURRENT);
		inactiveAccount.setBalance(new BigDecimal("500.00"));
		// Initial save triggers @PrePersist
		inactiveAccount = accountRepository.saveAndFlush(inactiveAccount);
		// Subsequent save updates fields bypasses @PrePersist
		inactiveAccount.setAccountStatus(AccountStatus.APPROVED);
		inactiveAccount.setIsActive(false);
		inactiveAccount = accountRepository.saveAndFlush(inactiveAccount);
	}

	@Test
	@DisplayName("Should return 200 and correct aggregations for valid owner")
	void shouldReturnCorrectAggregations() throws Exception {
		// Add CREDIT transaction on active account
		Transaction t1 = new Transaction();
		t1.setAccount(activeAccount);
		t1.setTransactionType(TransactionType.CREDIT);
		t1.setAmount(new BigDecimal("250.00"));
		t1.setBalanceAfter(new BigDecimal("1250.00"));
		t1.setDescription("Salary credit");
		t1.setTransactionDate(LocalDateTime.now());
		transactionRepository.saveAndFlush(t1);

		// Add DEBIT transaction on active account
		Transaction t2 = new Transaction();
		t2.setAccount(activeAccount);
		t2.setTransactionType(TransactionType.DEBIT);
		t2.setAmount(new BigDecimal("50.00"));
		t2.setBalanceAfter(new BigDecimal("1200.00"));
		t2.setDescription("Online purchase");
		t2.setTransactionDate(LocalDateTime.now());
		transactionRepository.saveAndFlush(t2);

		// Add transaction on INACTIVE account (should be ignored)
		Transaction t3 = new Transaction();
		t3.setAccount(inactiveAccount);
		t3.setTransactionType(TransactionType.CREDIT);
		t3.setAmount(new BigDecimal("1000.00"));
		t3.setBalanceAfter(new BigDecimal("1500.00"));
		t3.setDescription("Inactive account credit");
		t3.setTransactionDate(LocalDateTime.now());
		transactionRepository.saveAndFlush(t3);

		mockMvc.perform(get("/insights/" + user1.getId())
				.header("Authorization", "Bearer " + token1)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalIncome").value(250.00))
				.andExpect(jsonPath("$.totalExpense").value(50.00))
				.andExpect(jsonPath("$.savings").value(200.00))
				.andExpect(jsonPath("$.trendSummary").isArray())
				.andExpect(jsonPath("$.trendSummary.length()").value(6));
	}

	@Test
	@DisplayName("Should enforce cross-user privacy controls and return 403 Forbidden")
	void shouldBlockCrossUserAccess() throws Exception {
		mockMvc.perform(get("/insights/" + user1.getId())
				.header("Authorization", "Bearer " + token2)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Should return 401 Unauthorized for unauthenticated calls")
	void shouldBlockUnauthenticatedAccess() throws Exception {
		mockMvc.perform(get("/insights/" + user1.getId())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Should zero-pad months with no transactions in trendSummary")
	void shouldZeroPadMissingMonths() throws Exception {
		mockMvc.perform(get("/insights/" + user1.getId())
				.header("Authorization", "Bearer " + token1)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalIncome").value(0.00))
				.andExpect(jsonPath("$.totalExpense").value(0.00))
				.andExpect(jsonPath("$.savings").value(0.00))
				.andExpect(jsonPath("$.trendSummary[0].totalIncome").value(0.00))
				.andExpect(jsonPath("$.trendSummary[0].totalExpense").value(0.00));
	}
}
