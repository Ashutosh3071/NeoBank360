package com.neobank.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.neobank.entity.LoanRepayment;
import com.neobank.enums.RepaymentStatus;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {
    Page<LoanRepayment> findAllByLoanAccountId(Long loanAccountId, Pageable pageable);
    Page<LoanRepayment> findAllByLoanAccountIdAndPaymentStatus(Long loanAccountId, RepaymentStatus paymentStatus, Pageable pageable);
    List<LoanRepayment> findAllByLoanAccountId(Long loanAccountId);
    List<LoanRepayment> findAllByPaymentStatusAndDueDateBefore(RepaymentStatus paymentStatus, LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT lr.loanAccount.id) FROM LoanRepayment lr WHERE lr.paymentStatus = com.neobank.enums.RepaymentStatus.OVERDUE")
    long countOverdueLoanAccounts();
}
