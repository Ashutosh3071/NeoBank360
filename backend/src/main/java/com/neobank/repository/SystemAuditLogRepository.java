package com.neobank.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neobank.entity.SystemAuditLog;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {

	@Query("SELECT s FROM SystemAuditLog s WHERE " +
	       "(:status IS NULL OR s.responseStatus = :status) AND " +
	       "(:from IS NULL OR s.eventTimestamp >= :from) AND " +
	       "(:to IS NULL OR s.eventTimestamp <= :to)")
	Page<SystemAuditLog> findFilteredLogs(
			@Param("status") Integer status,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to,
			Pageable pageable
	);

	@Query("SELECT COUNT(DISTINCT s.actingUserId) FROM SystemAuditLog s WHERE s.eventTimestamp >= :timeLimit AND s.actingUserId IS NOT NULL")
	long countActiveUsers(@Param("timeLimit") LocalDateTime timeLimit);
}
