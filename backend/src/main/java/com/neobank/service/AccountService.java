package com.neobank.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.AccountResponse;
import com.neobank.dto.CreateAccountRequest;
import com.neobank.entity.Account;
import com.neobank.entity.User;
import com.neobank.enums.Role;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.UserRepository;

@Service
public class AccountService {

	// Each account type is limited to 1 per user (1 SAVINGS + 1 CURRENT max)

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UserRepository userRepository;

	/**
	 * ✅ Create new bank account ✅ Account will be PENDING_APPROVAL & INACTIVE
	 * (via @PrePersist)
	 */
	@Transactional
	public AccountResponse createAccount(CreateAccountRequest request) {

		User authenticatedUser = getAuthenticatedUser();

		// ✅ Admins cannot create bank accounts
		if (authenticatedUser.getRole() == Role.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Admins cannot create bank accounts. Only customers can open accounts.");
		}

		// ✅ One account per type: user can hold at most 1 SAVINGS and 1 CURRENT account
		if (accountRepository.existsByUserIdAndAccountType(authenticatedUser.getId(), request.getAccountType())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"You already have a " + request.getAccountType() + " account. Only one account per type is allowed.");
		}

		Account account = new Account();
		account.setUser(authenticatedUser);
		account.setAccountType(request.getAccountType());
		account.setBalance(BigDecimal.ZERO);
		account.setAccountNumber(generateUniqueAccountNumber());
		// ✅ accountStatus & isActive handled by Account.@PrePersist

		Account savedAccount = accountRepository.save(account);
		return toResponse(savedAccount);
	}

	/**
	 * ✅ Get all accounts of logged-in user (Approved + Pending included)
	 */
	@Transactional(readOnly = true)
	public List<AccountResponse> getMyAccounts() {
		Long userId = getAuthenticatedUser().getId();

		return accountRepository.findAllByUserId(userId).stream().map(this::toResponse).toList();
	}

	/**
	 * ✅ Get account by ID (ownership enforced)
	 */
	@Transactional(readOnly = true)
	public AccountResponse getMyAccountById(Long accountId) {

		User authenticatedUser = getAuthenticatedUser();

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		if (!account.getUser().getId().equals(authenticatedUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this account");
		}

		return toResponse(account);
	}

	/**
	 * ✅ Get account by number (ownership enforced)
	 */
	@Transactional(readOnly = true)
	public AccountResponse getMyAccountByNumber(String accountNumber) {

		User authenticatedUser = getAuthenticatedUser();

		Account account = accountRepository.findByAccountNumber(accountNumber)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		if (!account.getUser().getId().equals(authenticatedUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this account");
		}

		return toResponse(account);
	}

	// =========================
	// Helper methods
	// =========================

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

		return userRepository.findByEmail(email).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
	}

	private String generateUniqueAccountNumber() {

		String accountNumber;

		do {
			String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
			accountNumber = "NB" + raw.substring(0, 14);
		} while (accountRepository.existsByAccountNumber(accountNumber));

		return accountNumber;
	}

	private AccountResponse toResponse(Account account) {

		return new AccountResponse(account.getId(), account.getAccountNumber(), account.getAccountType(),
				account.getBalance(), account.getAccountStatus(), account.getIsActive(), account.getCreatedAt());
	}
}