package com.neobank.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.neobank.entity.LoanAccount;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    List<LoanAccount> findAllByUserId(Long userId);
    Optional<LoanAccount> findByLoanApplicationId(Long loanApplicationId);
}
