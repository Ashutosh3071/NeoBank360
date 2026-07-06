package com.neobank.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neobank.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	Page<Transaction> findAllByAccountId(Long accountId, Pageable pageable);

	Page<Transaction> findAll(Pageable pageable);

	List<Transaction> findByAccountIdAndTransactionDateBetween(Long accountId, LocalDateTime start, LocalDateTime end);

	@org.springframework.data.jpa.repository.Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId ORDER BY t.transactionDate DESC")
	List<Transaction> findRecentTransactionsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

	List<Transaction> findByTransactionDateGreaterThanEqual(LocalDateTime startDate);
}