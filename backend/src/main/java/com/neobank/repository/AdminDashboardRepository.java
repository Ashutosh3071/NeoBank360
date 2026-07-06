package com.neobank.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.neobank.entity.User;

@Repository
public interface AdminDashboardRepository extends JpaRepository<User, Long> {

	@Query("SELECT COUNT(u) FROM User u")
	long countAllUsers();

	@Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
	long countAllActiveUsers();

	@Query("SELECT COUNT(l) FROM LoanApplication l")
	long countAllLoans();

	@Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.status = com.neobank.enums.LoanStatus.PENDING")
	long countPendingLoans();

	@Query("SELECT COUNT(a) FROM Account a WHERE a.accountStatus = com.neobank.enums.AccountStatus.PENDING_APPROVAL")
	long countPendingAccounts();

	@Query("SELECT COUNT(t) FROM Transaction t")
	long countAllTransactions();

	@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN t.account a " +
	       "WHERE t.transactionType = com.neobank.enums.TransactionType.CREDIT AND a.isActive = true")
	BigDecimal getTotalIncomePlatform();

	@Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t JOIN t.account a " +
	       "WHERE t.transactionType = com.neobank.enums.TransactionType.DEBIT AND a.isActive = true")
	BigDecimal getTotalExpensePlatform();
}
