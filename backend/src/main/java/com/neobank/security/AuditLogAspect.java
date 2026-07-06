package com.neobank.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.entity.SystemAuditLog;
import com.neobank.repository.UserRepository;
import com.neobank.service.SystemAuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Component
public class AuditLogAspect {

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private HttpServletResponse response;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SystemAuditLogService auditLogService;

	@Around("execution(* com.neobank.controller..*(..))")
	public Object auditLog(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();
		Object result = null;
		int status = 200;
		String errorMessage = null;

		try {
			result = joinPoint.proceed();
			if (result instanceof ResponseEntity<?> responseEntity) {
				status = responseEntity.getStatusCode().value();
			} else {
				status = response.getStatus();
			}
			return result;
		} catch (Throwable t) {
			errorMessage = t.getMessage();
			if (t instanceof ResponseStatusException rse) {
				status = rse.getStatusCode().value();
			} else {
				status = 500;
			}
			throw t;
		} finally {
			long executionTimeMs = System.currentTimeMillis() - start;

			// Extract User ID
			Long actingUserId = null;
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
				String authHeader = request.getHeader("Authorization");
				if (authHeader != null && authHeader.startsWith("Bearer ")) {
					try {
						String token = authHeader.substring(7);
						actingUserId = jwtUtil.extractUserId(token);
					} catch (Exception e) {
						// ignore and fallback
					}
				}
				if (actingUserId == null) {
					try {
						String email = auth.getName();
						actingUserId = userRepository.findByEmail(email)
								.map(com.neobank.entity.User::getId)
								.orElse(null);
					} catch (Exception e) {
						// ignore
					}
				}
			}

			// Sanitise sensitive error details
			String sanitisedError = null;
			if (errorMessage != null) {
				if (errorMessage.contains("Bearer") || errorMessage.toLowerCase().contains("password")) {
					sanitisedError = "Sensitive data omitted";
				} else {
					sanitisedError = errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage;
				}
			}

			SystemAuditLog auditLog = new SystemAuditLog();
			auditLog.setEndpoint(request.getRequestURI());
			auditLog.setHttpMethod(request.getMethod());
			auditLog.setResponseStatus(status);
			auditLog.setExecutionTimeMs(executionTimeMs);
			auditLog.setActingUserId(actingUserId);
			auditLog.setErrorMessage(sanitisedError);

			auditLogService.saveLog(auditLog);
		}
	}
}
