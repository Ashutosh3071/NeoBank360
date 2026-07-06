package com.neobank.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.neobank.entity.Transaction;
import com.neobank.dto.TrendEntryDTO;

@Repository
public interface InsightsRepository extends JpaRepository<Transaction, Long> {

	@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN t.account a " +
	       "WHERE a.user.id = :userId AND t.transactionType = com.neobank.enums.TransactionType.CREDIT " +
	       "AND a.isActive = true")
	BigDecimal getTotalIncome(@Param("userId") Long userId);

	@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN t.account a " +
	       "WHERE a.user.id = :userId AND t.transactionType = com.neobank.enums.TransactionType.DEBIT " +
	       "AND a.isActive = true")
	BigDecimal getTotalExpense(@Param("userId") Long userId);

	@Query("SELECT t FROM Transaction t JOIN t.account a WHERE a.user.id = :userId AND a.isActive = true AND t.transactionDate >= :startDate")
	List<Transaction> findAllTransactionsForInsights(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
}
