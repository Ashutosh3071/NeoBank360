// src/main/java/com/neobank/repository/ApplicationRepository.java

package com.neobank.repository;

import com.neobank.entity.Application;
import com.neobank.entity.Application.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // ── Application ID generation ──
    @Query("SELECT a.applicationId FROM Application a WHERE a.applicationId LIKE :prefix% ORDER BY a.applicationId DESC LIMIT 1")
    Optional<String> findLastApplicationIdByPrefix(String prefix);

    // ── Active check ──
    @Query("""
        SELECT COUNT(a) > 0 FROM Application a
        WHERE (a.emailId = :email OR a.aadhaarNumber = :aadhaar OR a.panNumber = :pan)
        AND a.status IN (:activeStatuses)
    """)
    boolean existsActiveApplication(String email, String aadhaar, String pan, List<ApplicationStatus> activeStatuses);

    // ── Get all (no docs) — for admin list ──
    @Query("""
        SELECT a FROM Application a
        ORDER BY a.createdAt DESC
    """)
    Page<Application> findAllApplications(Pageable pageable);

    // ── Filter by status ──
    Page<Application> findByStatusOrderByCreatedAtDesc(ApplicationStatus status, Pageable pageable);

    // ── Get by applicationId ──
    Optional<Application> findByApplicationId(String applicationId);

    // ── Get by email ──
    List<Application> findByEmailIdOrderByCreatedAtDesc(String emailId);
}