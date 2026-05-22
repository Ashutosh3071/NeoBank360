// src/main/java/com/neobank/controller/TransactionController.java

package com.neobank.controller;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.transaction.*;
import com.neobank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

//    // Customer: Transfer / UPI / NEFT
//    @PostMapping("/transfer")
//    public ResponseEntity<ApiResponseDTO<TransactionResponseDTO>> transfer(
//            @RequestBody TransferRequestDTO req,
//            Authentication auth) {
//        var result = transactionService.transfer(req, auth.getName());
//        return ResponseEntity.ok(ApiResponseDTO.success("Transfer successful", result));
//    }
    
 // Replace /transfer endpoint with 3 separate ones:

    @PostMapping("/upi")
    public ResponseEntity<ApiResponseDTO<TransactionResponseDTO>> upiTransfer(
            @RequestBody UpiRequestDTO req, Authentication auth) {
        var result = transactionService.upiTransfer(req, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("UPI payment successful", result));
    }

    @PostMapping("/neft")
    public ResponseEntity<ApiResponseDTO<TransactionResponseDTO>> neftTransfer(
            @RequestBody NeftRequestDTO req, Authentication auth) {
        var result = transactionService.neftTransfer(req, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("NEFT transfer successful", result));
    }

    @PostMapping("/self")
    public ResponseEntity<ApiResponseDTO<TransactionResponseDTO>> selfTransfer(
            @RequestBody SelfTransferRequestDTO req, Authentication auth) {
        var result = transactionService.selfTransfer(req, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("Self transfer successful", result));
    }

    // Customer: my transactions
    @GetMapping("/my/{accountNumber}")
    public ResponseEntity<ApiResponseDTO<Page<TransactionResponseDTO>>> myTransactions(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Pageable pageable = PageRequest.of(page, size);
        var result = transactionService.getAccountTransactions(accountNumber, auth.getName(), pageable);
        return ResponseEntity.ok(ApiResponseDTO.success("Transactions fetched", result));
    }

    // Admin: deposit
    @PostMapping("/admin/deposit")
    public ResponseEntity<ApiResponseDTO<TransactionResponseDTO>> adminDeposit(
            @RequestBody AdminTransactionRequestDTO req,
            Authentication auth) {
        var result = transactionService.adminDeposit(req, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("Deposit successful", result));
    }

    // Admin: withdraw
    @PostMapping("/admin/withdraw")
    public ResponseEntity<ApiResponseDTO<TransactionResponseDTO>> adminWithdraw(
            @RequestBody AdminTransactionRequestDTO req,
            Authentication auth) {
        var result = transactionService.adminWithdraw(req, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("Withdrawal successful", result));
    }

    // Admin: all transactions
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponseDTO<Page<TransactionResponseDTO>>> allTransactions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(ApiResponseDTO.success("Transactions fetched", result));
    }
}