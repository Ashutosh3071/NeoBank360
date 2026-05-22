// BudgetController.java
package com.neobank.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.budget.BudgetRequestDTO;
import com.neobank.dto.budget.BudgetResponseDTO;
import com.neobank.dto.budget.BudgetSummaryResponseDTO;
import com.neobank.service.BudgetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<BudgetResponseDTO>> create(
            @RequestBody BudgetRequestDTO req, Authentication auth) {
        var result = budgetService.createBudget(req, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success("Budget created", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<BudgetResponseDTO>>> getAll(
            Authentication auth) {
        return ResponseEntity.ok(ApiResponseDTO.success("Budgets fetched",
                budgetService.getMyBudgets(auth.getName())));
    }

    @GetMapping("/{userId}/{month}")
    public ResponseEntity<ApiResponseDTO<BudgetSummaryResponseDTO>> getSummary(
            @PathVariable Long userId,
            @PathVariable String month,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponseDTO.success("Summary fetched",
                budgetService.getSummary(userId, month, auth.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> delete(
            @PathVariable Long id, Authentication auth) {
        budgetService.deleteBudget(id, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("Budget deleted", null));
    }
}