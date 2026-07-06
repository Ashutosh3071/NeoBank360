package com.neobank.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.neobank.dto.LoanApplicationRequestDTO;
import com.neobank.dto.LoanApplicationResponseDTO;
import com.neobank.dto.LoanDecisionDTO;
import com.neobank.enums.LoanStatus;
import com.neobank.service.LoanApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/loans")
@Tag(name = "Loan Applications", description = "Endpoints for applying and deciding on loans")
public class LoanApplicationController {

    @Autowired
    private LoanApplicationService loanApplicationService;

    @PostMapping("/apply")
    @Operation(summary = "Submit a loan application", description = "Submit a loan application validated against the loan product configuration. Ownership resolved from JWT payload.")
    @ApiResponse(responseCode = "201", description = "Loan application submitted successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or amount/tenure out of range")
    @ApiResponse(responseCode = "409", description = "Conflict - Duplicate application already exists")
    public ResponseEntity<LoanApplicationResponseDTO> apply(@RequestBody LoanApplicationRequestDTO request) {
        LoanApplicationResponseDTO response = loanApplicationService.apply(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-applications")
    @Operation(summary = "Get user's loan applications", description = "Retrieve list of all applications submitted by the logged-in customer")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved applications")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getMyApplications() {
        return ResponseEntity.ok(loanApplicationService.getMyApplications());
    }

    @GetMapping("/admin/applications")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all loan applications (Admin)", description = "Admin only endpoint to retrieve all loan applications, optional filtering by status.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved applications list")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getAdminApplications(
            @RequestParam(value = "status", required = false) LoanStatus status) {
        return ResponseEntity.ok(loanApplicationService.getAdminApplications(status));
    }

    @PutMapping("/{loanApplicationId}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Action a loan application", description = "Admin only endpoint to approve or reject a pending loan application. Atomic account and schedule creation on approval.")
    @ApiResponse(responseCode = "200", description = "Successfully updated application decision")
    @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    @ApiResponse(responseCode = "409", description = "Conflict - Application already decided")
    public ResponseEntity<LoanApplicationResponseDTO> decide(
            @PathVariable("loanApplicationId") Long loanApplicationId,
            @RequestBody LoanDecisionDTO request) {
        LoanApplicationResponseDTO response = loanApplicationService.decide(loanApplicationId, request);
        return ResponseEntity.ok(response);
    }
}
