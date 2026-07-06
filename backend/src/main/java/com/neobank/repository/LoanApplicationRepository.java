package com.neobank.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.neobank.entity.LoanApplication;
import com.neobank.enums.LoanStatus;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findAllByUserId(Long userId);
    List<LoanApplication> findAllByStatus(LoanStatus status);
    boolean existsByUserIdAndLoanProductIdAndStatus(Long userId, Long loanProductId, LoanStatus status);
    List<LoanApplication> findByAppliedAtGreaterThanEqual(java.time.LocalDateTime startDate);

    @Query(value = "SELECT disbursement_account_id FROM loan_applications WHERE id = :appId", nativeQuery = true)
    Long findDisbursementAccountIdById(@Param("appId") Long appId);
}
