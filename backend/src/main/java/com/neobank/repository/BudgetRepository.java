package com.neobank.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neobank.entity.Budget;
import com.neobank.enums.BudgetCategory;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserIdAndCategoryAndBudgetMonth(Long userId, BudgetCategory category, LocalDate budgetMonth);

    List<Budget> findAllByUserIdAndBudgetMonth(Long userId, LocalDate budgetMonth);

    List<Budget> findAllByUserId(Long userId);
}
