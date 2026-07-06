package com.neobank.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.neobank.entity.Bill;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findAllByUserId(Long userId);

    Optional<Bill> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT b FROM Bill b WHERE b.user.id = :userId AND b.billerName = :billerName " +
           "AND YEAR(b.dueDate) = :year AND MONTH(b.dueDate) = :month")
    Optional<Bill> findDuplicate(Long userId, String billerName, int year, int month);

    List<Bill> findAllByUserIdAndDueDateBetween(Long userId, LocalDate start, LocalDate end);
}
