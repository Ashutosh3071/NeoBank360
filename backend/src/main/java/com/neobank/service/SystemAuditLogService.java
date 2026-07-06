package com.neobank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neobank.entity.SystemAuditLog;
import com.neobank.repository.SystemAuditLogRepository;

@Service
public class SystemAuditLogService {

	@Autowired
	private SystemAuditLogRepository systemAuditLogRepository;

	@Async
	@Transactional
	public void saveLog(SystemAuditLog auditLog) {
		systemAuditLogRepository.save(auditLog);
	}
}
