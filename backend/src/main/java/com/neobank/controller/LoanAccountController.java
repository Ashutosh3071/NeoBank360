package com.neobank.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.LoanAccountDTO;
import com.neobank.service.LoanAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/loans")
@Tag(name = "Loan Accounts", description = "Endpoints for retrieving active loan accounts")
public class LoanAccountController {

    @Autowired
    private LoanAccountService loanAccountService;

    @GetMapping("/my-accounts")
    @Operation(summary = "Get user's active loan accounts", description = "Retrieve list of all active loan accounts for the logged-in user")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active accounts")
    public ResponseEntity<List<LoanAccountDTO>> getMyAccounts() {
        return ResponseEntity.ok(loanAccountService.getMyAccounts());
    }
}
