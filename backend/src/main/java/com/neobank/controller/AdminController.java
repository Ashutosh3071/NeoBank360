package com.neobank.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.AdminDashboardDTO;
import com.neobank.dto.PendingApprovalDTO;
import com.neobank.dto.TransactionResponse;
import com.neobank.dto.UserActivityDTO;
import com.neobank.dto.AdminTransactionAnalyticsDTO;
import com.neobank.dto.AdminLoanAnalyticsDTO;
import com.neobank.entity.SystemAuditLog;
import com.neobank.service.AdminService;

@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private AdminService adminService;

	// =========================
	// DASHBOARD
	// =========================

	@GetMapping("/dashboard")
	public AdminDashboardDTO getDashboardStats() {
		return adminService.getDashboardStats();
	}

	// =========================
	// SYSTEM HEALTH
	// =========================

	@GetMapping("/system-health")
	public Map<String, Object> getSystemHealth() {
		return adminService.getSystemHealth();
	}

	// =========================
	// PENDING APPROVALS (COMBINED)
	// =========================

	@GetMapping("/pending-approvals")
	public List<PendingApprovalDTO> getPendingApprovals(@RequestParam(required = false) String module) {
		return adminService.getPendingApprovals(module);
	}

	// =========================
	// USERS (SUPPORT PAGINATION & DETAILS)
	// =========================

	@GetMapping("/users")
	public ResponseEntity<?> getAllUsers(
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		if (page != null && size != null) {
			return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, size)));
		}
		return ResponseEntity.ok(adminService.getAllUsers());
	}

	@PatchMapping("/users/{userId}/status")
	public ResponseEntity<Map<String, String>> updateUserStatus(
			@PathVariable Long userId,
			@RequestBody Map<String, Boolean> payload,
			java.security.Principal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
		}

		Boolean isActive = payload.get("isActive");
		if (isActive == null) {
			isActive = payload.get("active"); // fallback
		}
		if (isActive == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isActive parameter is required");
		}

		adminService.updateUserStatus(userId, isActive, principal.getName());
		return ResponseEntity.ok(Map.of("message", "User status updated successfully"));
	}

	@GetMapping("/users/{userId}/activity")
	public ResponseEntity<UserActivityDTO> getUserActivity(@PathVariable Long userId) {
		return ResponseEntity.ok(adminService.getUserActivity(userId));
	}

	// =========================
	// ACCOUNTS
	// =========================

	@GetMapping("/accounts")
	public List<Map<String, Object>> getAllAccounts() {
		return adminService.getAllAccounts();
	}

	// =========================
	// ACCOUNT APPROVAL (LEGACY COMPATIBILITY)
	// =========================

	@GetMapping("/accounts/pending")
	public List<Map<String, Object>> getPendingAccounts() {
		return adminService.getPendingAccounts();
	}

	@GetMapping("/accounts/{id}")
	public Map<String, Object> getAccountDetail(@PathVariable Long id) {
		return adminService.getAccountDetail(id);
	}

	@PutMapping("/accounts/{id}/approve")
	public void approveAccount(@PathVariable Long id) {
		adminService.approveAccount(id);
	}

	@PutMapping("/accounts/{id}/reject")
	public void rejectAccount(@PathVariable Long id) {
	    adminService.rejectAccount(id);
	}

	/**
	 * ✅ Admin credits money directly into any approved user account
	 * POST /api/admin/accounts/{id}/credit
	 * Body: { "amount": 500.00, "description": "Bonus credit" }
	 */
	@PostMapping("/accounts/{id}/credit")
	public ResponseEntity<TransactionResponse> adminCreditAccount(
	        @PathVariable Long id,
	        @RequestBody Map<String, Object> payload) {
	    java.math.BigDecimal amount = new java.math.BigDecimal(payload.get("amount").toString());
	    String description = payload.containsKey("description") ? payload.get("description").toString() : "Admin credit";
	    TransactionResponse response = adminService.adminCreditAccount(id, amount, description);
	    return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// =========================
	// TRANSACTIONS
	// =========================

	@GetMapping("/transactions/recent")
	public List<TransactionResponse> getRecentTransactions(@RequestParam(defaultValue = "10") int size) {
		return adminService.getRecentTransactions(size);
	}

	// =========================
	// SPRINT 5 ANALYTICS & LOGS
	// =========================

	@GetMapping("/analytics/transactions")
	public AdminTransactionAnalyticsDTO getTransactionsAnalytics(@RequestParam(defaultValue = "7d") String timeframe) {
		return adminService.getTransactionsAnalytics(timeframe);
	}

	@GetMapping("/analytics/loans")
	public AdminLoanAnalyticsDTO getLoansAnalytics(@RequestParam(defaultValue = "7d") String timeframe) {
		return adminService.getLoansAnalytics(timeframe);
	}

	@GetMapping("/system-logs")
	public Page<SystemAuditLog> getSystemLogs(
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(required = false) Integer status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		java.time.LocalDateTime fromDate = null;
		java.time.LocalDateTime toDate = null;

		if (from != null && !from.trim().isEmpty()) {
			try {
				fromDate = java.time.LocalDateTime.parse(from);
			} catch (Exception e) {
				try {
					fromDate = java.time.LocalDate.parse(from).atStartOfDay();
				} catch (Exception ex) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid from date format. Use YYYY-MM-DD or YYYY-MM-DDTHH:mm:ss");
				}
			}
		}

		if (to != null && !to.trim().isEmpty()) {
			try {
				toDate = java.time.LocalDateTime.parse(to);
			} catch (Exception e) {
				try {
					toDate = java.time.LocalDate.parse(to).atTime(23, 59, 59, 999999999);
				} catch (Exception ex) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid to date format. Use YYYY-MM-DD or YYYY-MM-DDTHH:mm:ss");
				}
			}
		}

		return adminService.getSystemLogs(fromDate, toDate, status, PageRequest.of(page, size));
	}
}
