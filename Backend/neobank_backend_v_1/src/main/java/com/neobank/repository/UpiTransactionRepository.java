// UpiTransactionRepository.java
package com.neobank.repository;

import com.neobank.entity.UpiTransaction;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UpiTransactionRepository extends JpaRepository<UpiTransaction, Long> {

    Page<UpiTransaction> findBySenderVpaOrReceiverVpaOrderByCreatedAtDesc(
            String senderVpa, String receiverVpa, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM UpiTransaction t
        WHERE t.senderVpa = :vpa
        AND t.status = 'SUCCESS'
        AND DATE(t.createdAt) = :today
        """)
    BigDecimal getTodayUsedLimit(String vpa, LocalDate today);
    
    @Query("SELECT u.upiReference FROM UpiTransaction u " +
    	       "WHERE u.upiReference LIKE :prefix% " +
    	       "ORDER BY u.upiReference DESC LIMIT 1")
    	Optional<String> findLastReferenceByPrefix(@Param("prefix") String prefix);
}