package com.neobank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neobank.dto.LoanRepaymentDTO;
import com.neobank.service.LoanRepaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/loans")
@Tag(name = "Loan Repayments", description = "Endpoints for retrieving schedule details and paying installments")
public class LoanRepaymentController {

    @Autowired
    private LoanRepaymentService loanRepaymentService;

    @GetMapping("/{loanAccountId}/repayments")
    @Operation(summary = "Get repayment schedule", description = "Retrieve paginated amortization schedule for a specific loan account. Checks JWT ownership.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved repayment schedule")
    @ApiResponse(responseCode = "403", description = "Forbidden - Mismatch in JWT ownership")
    @ApiResponse(responseCode = "404", description = "Loan account not found")
    public ResponseEntity<Page<LoanRepaymentDTO>> getRepaymentsForAccount(
            @PathVariable("loanAccountId") Long loanAccountId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<LoanRepaymentDTO> response = loanRepaymentService.getRepaymentsForAccount(loanAccountId, status, page, size);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{loanAccountId}/repayments/{repaymentId}/pay")
    @Operation(summary = "Pay a loan installment", description = "Mark a specific installment as PAID. Ownership and status are validated.")
    @ApiResponse(responseCode = "200", description = "Installment paid successfully")
    @ApiResponse(responseCode = "400", description = "Installment already paid or mismatch in account details")
    @ApiResponse(responseCode = "403", description = "Forbidden - Mismatch in account ownership")
    @ApiResponse(responseCode = "404", description = "Repayment or Account not found")
    public ResponseEntity<LoanRepaymentDTO> payInstallment(
            @PathVariable("loanAccountId") Long loanAccountId,
            @PathVariable("repaymentId") Long repaymentId,
            @RequestParam("accountId") Long accountId) {
        LoanRepaymentDTO response = loanRepaymentService.payInstallment(loanAccountId, repaymentId, accountId);
        return ResponseEntity.ok(response);
    }
}
