// BudgetRepository.java
package com.neobank.repository;

import com.neobank.entity.Budget;
import com.neobank.entity.Budget.BudgetCategory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdOrderByBudgetMonthDesc(Long userId);

    List<Budget> findByUserIdAndBudgetMonthOrderByCategoryAsc(
            Long userId, LocalDate budgetMonth);

    Optional<Budget> findByUserIdAndCategoryAndBudgetMonth(
            Long userId, BudgetCategory category, LocalDate budgetMonth);

    boolean existsByUserIdAndCategoryAndBudgetMonth(
            Long userId, BudgetCategory category, LocalDate budgetMonth);
}