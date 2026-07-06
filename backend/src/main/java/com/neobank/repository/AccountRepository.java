package com.neobank.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.neobank.entity.Account;
import com.neobank.enums.AccountStatus;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {

	// ✅ Existing methods (UNCHANGED)

	List<Account> findAllByUserId(Long userId);

	Optional<Account> findByAccountNumber(String accountNumber);

	boolean existsByAccountNumber(String accountNumber);

	long countByUserId(Long userId);

	boolean existsByUserIdAndAccountType(Long userId, com.neobank.enums.AccountType accountType);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Account a where a.id = :id")
	Optional<Account> findByIdForUpdate(Long id);

	// ✅ NEW: Required for account approval workflow
	List<Account> findByAccountStatus(AccountStatus accountStatus);
}