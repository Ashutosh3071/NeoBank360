// BillRepository.java
package com.neobank.repository;

import com.neobank.entity.Bill;
import com.neobank.entity.Bill.BillStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByUserIdOrderByDueDateAsc(Long userId);

    // Duplicate check: same user + biller + month + year
    @Query("""
        SELECT COUNT(b) > 0 FROM Bill b
        WHERE b.user.id    = :userId
        AND   b.billerName = :billerName
        AND   YEAR(b.dueDate)  = :year
        AND   MONTH(b.dueDate) = :month
        """)
    boolean existsDuplicate(
            @Param("userId")     Long userId,
            @Param("billerName") String billerName,
            @Param("year")       int year,
            @Param("month")      int month);

    // Bills due within N days (for reminders)
    @Query("""
        SELECT b FROM Bill b
        WHERE b.user.id  = :userId
        AND   b.status   = 'PENDING'
        AND   b.dueDate >= :today
        AND   b.dueDate <= :threshold
        """)
    List<Bill> findUpcoming(
            @Param("userId")    Long userId,
            @Param("today")     LocalDate today,
            @Param("threshold") LocalDate threshold);

    // All overdue bills
    @Query("""
        SELECT b FROM Bill b
        WHERE b.user.id = :userId
        AND   b.status  = 'PENDING'
        AND   b.dueDate < :today
        """)
    List<Bill> findOverdue(
            @Param("userId") Long userId,
            @Param("today")  LocalDate today);
}