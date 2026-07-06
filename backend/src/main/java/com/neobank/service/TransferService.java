package com.neobank.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.TransferRequest;
import com.neobank.dto.TransferResponse;
import com.neobank.entity.Account;
import com.neobank.entity.Transaction;
import com.neobank.entity.User;
import com.neobank.enums.AccountStatus;
import com.neobank.enums.TransactionType;
import com.neobank.entity.Reward;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;

@Service
public class TransferService {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RewardService rewardService;

	@Transactional
	public TransferResponse transferMoney(TransferRequest request) {
		User authenticatedUser = getAuthenticatedUser();

		if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
		}

		// First fetch non-locking for validation/ordering
		Account sourcePreview = accountRepository.findById(request.getSourceAccountId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source account not found"));

		Account destinationPreview = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination account not found"));

		if (!sourcePreview.getUser().getId().equals(authenticatedUser.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"You are not allowed to transfer from this account");
		}

		// ✅ Block transfers from unapproved accounts
		if (!Boolean.TRUE.equals(sourcePreview.getIsActive())
				|| sourcePreview.getAccountStatus() != AccountStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Source account is not yet approved by admin. Transfers are disabled.");
		}

		if (!Boolean.TRUE.equals(destinationPreview.getIsActive())
				|| destinationPreview.getAccountStatus() != AccountStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Destination account is not yet approved. Cannot receive transfers.");
		}

		if (sourcePreview.getId().equals(destinationPreview.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Source and destination accounts must be different");
		}

		// Lock in deterministic order to prevent deadlocks
		Long firstId = Math.min(sourcePreview.getId(), destinationPreview.getId());
		Long secondId = Math.max(sourcePreview.getId(), destinationPreview.getId());

		Account firstLocked = accountRepository.findByIdForUpdate(firstId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		Account secondLocked = accountRepository.findByIdForUpdate(secondId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		Account source = firstLocked.getId().equals(sourcePreview.getId()) ? firstLocked : secondLocked;
		Account destination = firstLocked.getId().equals(destinationPreview.getId()) ? firstLocked : secondLocked;

		BigDecimal amount = request.getAmount();

		if (source.getBalance().compareTo(amount) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		BigDecimal sourceNewBalance = source.getBalance().subtract(amount);
		BigDecimal destinationNewBalance = destination.getBalance().add(amount);

		source.setBalance(sourceNewBalance);
		destination.setBalance(destinationNewBalance);

		accountRepository.save(source);
		accountRepository.save(destination);

		String referenceId = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		LocalDateTime now = LocalDateTime.now();

		Transaction sourceTx = new Transaction();
		sourceTx.setAccount(source);
		sourceTx.setTransactionType(TransactionType.TRANSFER);
		sourceTx.setAmount(amount);
		sourceTx.setBalanceAfter(sourceNewBalance);
		sourceTx.setDescription(
				buildSourceDescription(destination.getAccountNumber(), request.getDescription(), referenceId));
		sourceTx.setTransactionDate(now);

		Transaction destinationTx = new Transaction();
		destinationTx.setAccount(destination);
		destinationTx.setTransactionType(TransactionType.TRANSFER);
		destinationTx.setAmount(amount);
		destinationTx.setBalanceAfter(destinationNewBalance);
		destinationTx.setDescription(
				buildDestinationDescription(source.getAccountNumber(), request.getDescription(), referenceId));
		destinationTx.setTransactionDate(now);

		transactionRepository.save(sourceTx);
		transactionRepository.save(destinationTx);

		// Award reward points for completing transfers (+50 pts base)
		int pointsEarned = rewardService.awardPointsForAction(authenticatedUser, "TRANSFER", 50);

		return new TransferResponse(referenceId, source.getAccountNumber(), destination.getAccountNumber(), amount,
				sourceNewBalance, destinationNewBalance, now, pointsEarned);
	}

	private String buildSourceDescription(String destinationAccountNumber, String description, String referenceId) {
		String base = "Transfer to " + destinationAccountNumber + " | Ref: " + referenceId;
		if (description == null || description.isBlank()) {
			return base;
		}
		return base + " | " + description.trim();
	}

	private String buildDestinationDescription(String sourceAccountNumber, String description, String referenceId) {
		String base = "Transfer from " + sourceAccountNumber + " | Ref: " + referenceId;
		if (description == null || description.isBlank()) {
			return base;
		}
		return base + " | " + description.trim();
	}

	private User getAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Authentication is required to access this resource");
		}

		Object principal = authentication.getPrincipal();
		String email;

		if (principal instanceof UserDetails userDetails) {
			email = userDetails.getUsername();
		} else if (principal instanceof String str && !"anonymousUser".equals(str)) {
			email = str;
		} else {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Authentication is required to access this resource");
		}

		return userRepository.findByEmail(email).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
	}
}