// src/main/java/com/neobank/repository/UpiIdRepository.java

package com.neobank.repository;

import com.neobank.entity.UpiId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UpiIdRepository extends JpaRepository<UpiId, Long> {

    Optional<UpiId> findByVpa(String vpa);

    boolean existsByVpa(String vpa);

    // All active UPI IDs for an account
    List<UpiId> findByAccountIdAndStatusNot(Long accountId, UpiId.UpiStatus status);

    // All active UPI IDs for a user (across all accounts)
    List<UpiId> findByUserIdAndStatusNot(Long userId, UpiId.UpiStatus status);

    Optional<UpiId> findByVpaAndStatus(String vpa, UpiId.UpiStatus status);

    // Primary UPI ID for an account
    @Query("""
        SELECT u FROM UpiId u
        WHERE u.account.id = :accountId
        AND   u.isPrimary  = true
        AND   u.status     = 'ACTIVE'
        """)
    Optional<UpiId> findPrimaryByAccountId(@Param("accountId") Long accountId);

    // ── Resolve by phone number ──
    // Find the primary UPI ID linked to the account that belongs to user with this phone
    @Query("""
        SELECT u FROM UpiId u
        WHERE u.user.phone = :phone
        AND   u.isPrimary  = true
        AND   u.status     = 'ACTIVE'
        ORDER BY u.createdAt DESC
        LIMIT 1
        """)
    Optional<UpiId> findPrimaryByUserPhone(@Param("phone") String phone);

    // Count active UPI IDs for account (for max 5 limit check)
    @Query("""
        SELECT COUNT(u) FROM UpiId u
        WHERE u.account.id = :accountId
        AND   u.status    != 'DELETED'
        """)
    long countActiveByAccountId(@Param("accountId") Long accountId);
}