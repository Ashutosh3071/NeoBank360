// AccountRequestRepository.java
package com.neobank.repository;

import com.neobank.entity.AccountRequest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {

    Page<AccountRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AccountRequest> findByStatusOrderByCreatedAtDesc(
            AccountRequest.RequestStatus status, Pageable pageable);

    Optional<AccountRequest> findByRequestId(String requestId);

    @Query("SELECT COUNT(r) > 0 FROM AccountRequest r WHERE r.user.id = :userId AND r.status = 'PENDING'")
    boolean hasPendingRequest(Long userId);

    Page<AccountRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT r FROM AccountRequest r WHERE r.user.id = :userId ORDER BY r.createdAt DESC LIMIT 1")
    Optional<AccountRequest> findLatestByUserId(Long userId);
}