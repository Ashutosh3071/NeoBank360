package com.neobank.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.neobank.dto.LoanProductDTO;
import com.neobank.service.LoanProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/loans/products")
@Tag(name = "Loan Products", description = "Endpoints for configuring and viewing loan products")
public class LoanProductController {

    @Autowired
    private LoanProductService loanProductService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new loan product", description = "Admin only endpoint to configure available loan products")
    @ApiResponse(responseCode = "201", description = "Loan product created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "403", description = "Forbidden - Only admins can configure loan products")
    @ApiResponse(responseCode = "499", description = "Conflict - Product name already exists")
    public ResponseEntity<LoanProductDTO> createProduct(@RequestBody LoanProductDTO request) {
        LoanProductDTO response = loanProductService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all loan products", description = "Retrieve list of all configured loan products")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved products")
    public ResponseEntity<List<LoanProductDTO>> getAllProducts() {
        return ResponseEntity.ok(loanProductService.getAllProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan product by ID", description = "Retrieve details of a specific loan product by its unique ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved product")
    @ApiResponse(responseCode = "404", description = "Loan product not found")
    public ResponseEntity<LoanProductDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(loanProductService.getProductById(id));
    }
}
