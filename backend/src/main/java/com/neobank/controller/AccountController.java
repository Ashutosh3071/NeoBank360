package com.neobank.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.AccountResponse;
import com.neobank.dto.CreateAccountRequest;
import com.neobank.service.AccountService;

@RestController
@RequestMapping("/accounts")

public class AccountController {
	@Autowired
	private AccountService accountService;

	@PostMapping
	public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
		AccountResponse response = accountService.createAccount(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<AccountResponse>> getMyAccounts() {
		return ResponseEntity.ok(accountService.getMyAccounts());
	}

	@GetMapping("/my")
	public ResponseEntity<List<AccountResponse>> getMyAccountsAlias() {
		return ResponseEntity.ok(accountService.getMyAccounts());
	}

	@GetMapping("/{id}")
	public ResponseEntity<AccountResponse> getMyAccountById(@PathVariable Long id) {
		return ResponseEntity.ok(accountService.getMyAccountById(id));
	}
}