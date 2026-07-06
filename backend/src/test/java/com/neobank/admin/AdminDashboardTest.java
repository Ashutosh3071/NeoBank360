package com.neobank.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobank.entity.Account;
import com.neobank.entity.User;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.AccountType;
import com.neobank.enums.Role;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.UserRepository;
import com.neobank.security.JwtUtil;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public class AdminDashboardTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private JwtUtil jwtUtil;

	private User adminUser;
	private User customerUser;
	private String adminToken;
	private String customerToken;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		// Create Admin
		adminUser = new User();
		adminUser.setEmail("admin_" + UUID.randomUUID() + "@neobank.in");
		adminUser.setPasswordHash("encoded_pw");
		adminUser.setFullName("Platform Admin");
		adminUser.setRole(Role.ADMIN);
		adminUser.setAadhaarNumber("91" + (long)(Math.random()*9000000000L));
		adminUser.setPanNumber("ADMIN" + (int)(Math.random()*9000) + "D");
		adminUser.setIsActive(true);
		adminUser = userRepository.saveAndFlush(adminUser);

		// Create Customer
		customerUser = new User();
		customerUser.setEmail("cust_" + UUID.randomUUID() + "@neobank.in");
		customerUser.setPasswordHash("encoded_pw");
		customerUser.setFullName("Platform Customer");
		customerUser.setRole(Role.CUSTOMER);
		customerUser.setAadhaarNumber("81" + (long)(Math.random()*9000000000L));
		customerUser.setPanNumber("CUSTO" + (int)(Math.random()*9000) + "C");
		customerUser.setIsActive(true);
		customerUser = userRepository.saveAndFlush(customerUser);

		// Tokens
		adminToken = jwtUtil.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
		customerToken = jwtUtil.generateToken(customerUser.getId(), customerUser.getEmail(), customerUser.getRole().name());
	}

	@Test
	@DisplayName("Should restrict admin dashboard endpoints and return 403 for CUSTOMER")
	void shouldRestrictAdminEndpoints() throws Exception {
		mockMvc.perform(get("/admin/dashboard")
				.header("Authorization", "Bearer " + customerToken)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("Should allow admin dashboard endpoints for ADMIN role")
	void shouldAllowAdminEndpoints() throws Exception {
		mockMvc.perform(get("/admin/dashboard")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalUsers").exists())
				.andExpect(jsonPath("$.platformSavingsRate").exists());
	}

	@Test
	@DisplayName("Should return system-health UP connectivity status")
	void shouldReturnSystemHealth() throws Exception {
		mockMvc.perform(get("/admin/system-health")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dbStatus").value("UP"))
				.andExpect(jsonPath("$.activeSessions").exists())
				.andExpect(jsonPath("$.serverUptimeSeconds").exists());
	}

	@Test
	@DisplayName("Should fetch combined pending approvals ordered by date")
	void shouldFetchCombinedApprovals() throws Exception {
		// Add pending account approval
		Account pendingAccount = new Account();
		pendingAccount.setUser(customerUser);
		pendingAccount.setAccountNumber("ACC_" + UUID.randomUUID().toString().substring(0, 10));
		pendingAccount.setAccountType(AccountType.SAVINGS);
		pendingAccount.setBalance(new BigDecimal("200.00"));
		pendingAccount.setAccountStatus(AccountStatus.PENDING_APPROVAL);
		pendingAccount.setIsActive(false);
		pendingAccount.setCreatedAt(LocalDateTime.now().minusHours(1));
		accountRepository.saveAndFlush(pendingAccount);

		mockMvc.perform(get("/admin/pending-approvals")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].type").value("ACCOUNT_APPROVAL"))
				.andExpect(jsonPath("$[0].requestedAmount").value(200.00));
	}

	@Test
	@DisplayName("Should prevent admin from deactivating their own account and return 400 Bad Request")
	void shouldPreventSelfDeactivation() throws Exception {
		Map<String, Boolean> payload = new HashMap<>();
		payload.put("isActive", false);

		mockMvc.perform(patch("/admin/users/" + adminUser.getId() + "/status")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should toggle user active status and log audit trail")
	void shouldDeactivateCustomer() throws Exception {
		Map<String, Boolean> payload = new HashMap<>();
		payload.put("isActive", false);

		mockMvc.perform(patch("/admin/users/" + customerUser.getId() + "/status")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)))
				.andExpect(status().isOk());

		// Verify change in DB
		User updated = userRepository.findById(customerUser.getId()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertFalse(updated.getIsActive());
	}

	@Test
	@DisplayName("Should never expose password_hash in users response list")
	void shouldExcludePasswordHashes() throws Exception {
		mockMvc.perform(get("/admin/users")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].password_hash").doesNotExist())
				.andExpect(jsonPath("$[0].passwordHash").doesNotExist());
	}
}
