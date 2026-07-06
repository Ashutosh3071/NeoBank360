package com.neobank.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.CreateTransactionRequest;
import com.neobank.dto.TransactionResponse;
import com.neobank.entity.Account;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.Role;
import com.neobank.enums.TransactionType;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;

@Service
public class TransactionService {

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UserRepository userRepository;

	// =========================
	// VIEW TRANSACTIONS
	// =========================

	@Transactional(readOnly = true)
	public Page<TransactionResponse> getMyAccountTransactions(Long accountId, int page, int size) {

		User authenticatedUser = getAuthenticatedUser();

		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		validateOwnership(account, authenticatedUser);

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));

		return transactionRepository.findAllByAccountId(accountId, pageable).map(this::toResponse);
	}

	// =========================
	// CREATE TRANSACTION
	// =========================

	@Transactional
	public TransactionResponse createTransaction(Long accountId, CreateTransactionRequest request) {

		User authenticatedUser = getAuthenticatedUser();

		Account account = accountRepository.findByIdForUpdate(accountId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		validateOwnership(account, authenticatedUser);

		// ✅ CRITICAL: BLOCK UNAPPROVED ACCOUNTS
		if (!Boolean.TRUE.equals(account.getIsActive()) || account.getAccountStatus() != AccountStatus.APPROVED) {

			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not approved by admin");
		}

		BigDecimal amount = request.getAmount();

		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
		}

		// ✅ CREDIT restriction: the account owner cannot credit their own account.
		//    Only an admin or another user (via transfer) may credit an account.
		//    Debits by the account owner are still allowed.
		if (request.getTransactionType() == TransactionType.CREDIT) {
			boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;
			boolean isOwner = account.getUser().getId().equals(authenticatedUser.getId());
			if (isOwner && !isAdmin) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
						"You cannot credit your own account. Credits must come from an admin or another user.");
			}
		}

		BigDecimal currentBalance = account.getBalance();
		BigDecimal updatedBalance;

		if (request.getTransactionType() == TransactionType.CREDIT) {
			updatedBalance = currentBalance.add(amount);

		} else if (request.getTransactionType() == TransactionType.DEBIT) {

			if (currentBalance.compareTo(amount) < 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
			}

			updatedBalance = currentBalance.subtract(amount);

		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported transaction type");
		}

		account.setBalance(updatedBalance);
		accountRepository.save(account);

		Transaction transaction = new Transaction();
		transaction.setAccount(account);
		transaction.setTransactionType(request.getTransactionType());
		transaction.setAmount(amount);
		transaction.setBalanceAfter(updatedBalance);
		transaction.setDescription(request.getDescription());

		Transaction savedTransaction = transactionRepository.save(transaction);

		return toResponse(savedTransaction);
	}

	// =========================
	// HELPERS
	// =========================

	private void validateOwnership(Account account, User authenticatedUser) {
		// Admins can access any account (e.g., to credit a user's account)
		if (authenticatedUser.getRole() == Role.ADMIN) {
			return;
		}
		if (!account.getUser().getId().equals(authenticatedUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access this account");
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

		return userRepository.findByEmail(email).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
	}

	private TransactionResponse toResponse(Transaction transaction) {

		return new TransactionResponse(transaction.getId(), transaction.getAccount().getId(),
				transaction.getAccount().getAccountNumber(), transaction.getTransactionType(), transaction.getAmount(),
				transaction.getBalanceAfter(), transaction.getDescription(), transaction.getTransactionDate());
	}
}