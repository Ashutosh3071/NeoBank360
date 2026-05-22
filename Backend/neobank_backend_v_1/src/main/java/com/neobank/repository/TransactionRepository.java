// src/main/java/com/neobank/repository/TransactionRepository.java
package com.neobank.repository;

import com.neobank.entity.Transaction;
import com.neobank.entity.Transaction.TransactionStatus;
import com.neobank.entity.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // For reference number generation
    @Query("SELECT t.referenceNumber FROM Transaction t WHERE t.referenceNumber LIKE :prefix% ORDER BY t.referenceNumber DESC LIMIT 1")
    Optional<String> findLastReferenceByPrefix(@Param("prefix") String prefix);

    // Customer - my transactions
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    // Admin - all transactions with optional filters
    Page<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status, Pageable pageable);
    Page<Transaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    // Stats
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.toAccount.id = :accountId AND t.status = 'SUCCESS' AND t.transactionType = 'DEPOSIT'")
    BigDecimal getTotalDeposits(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.fromAccount.id = :accountId AND t.status = 'SUCCESS'")
    BigDecimal getTotalWithdrawals(@Param("accountId") Long accountId);

    // ✅ FIXED: was missing @Query — Spring couldn't derive it from the method name
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.fromAccount.id = :accountId " +
           "AND t.status = 'SUCCESS' " +
           "AND CAST(t.createdAt AS date) = :date")
    BigDecimal getTodayTotal(@Param("accountId") Long accountId, @Param("date") LocalDate date);
    
 // Add to TransactionRepository.java

 // Debit transactions for an account in a date range
 @Query("""
     SELECT t FROM Transaction t
     WHERE t.fromAccount.id = :accountId
     AND   t.status         = 'SUCCESS'
     AND   t.createdAt     >= :from
     AND   t.createdAt      < :to
     """)
 List<Transaction> findDebitsByAccountAndDateRange(
         @Param("accountId") Long accountId,
         @Param("from")      java.time.LocalDateTime from,
         @Param("to")        java.time.LocalDateTime to);
}