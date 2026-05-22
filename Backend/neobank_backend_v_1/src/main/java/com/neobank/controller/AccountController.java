// src/main/java/com/neobank/controller/AccountController.java

package com.neobank.controller;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.account.*;
import com.neobank.entity.Account.AccountStatus;
import com.neobank.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    // Customer: my accounts
    @GetMapping("/my")
    public ResponseEntity<ApiResponseDTO<List<AccountResponseDTO>>> myAccounts(
            Authentication auth) {
        var result = accountService.getMyAccounts(auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("Accounts fetched", result));
    }

    // Admin: all accounts
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponseDTO<Page<AccountResponseDTO>>> allAccounts(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        var result = accountService.getAllAccounts(status, pageable);
        return ResponseEntity.ok(ApiResponseDTO.success("Accounts fetched", result));
    }

    // Admin: update status
    @PutMapping("/admin/status")
    public ResponseEntity<ApiResponseDTO<AccountResponseDTO>> updateStatus(
            @RequestBody AccountStatusUpdateDTO req,
            Authentication auth) {
        var result = accountService.updateAccountStatus(req, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("Account status updated", result));
    }
}