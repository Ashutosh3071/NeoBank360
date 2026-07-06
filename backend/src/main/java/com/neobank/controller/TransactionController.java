package com.neobank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.CreateTransactionRequest;
import com.neobank.dto.TransactionResponse;
import com.neobank.service.TransactionService;

@RestController
@RequestMapping("/accounts/{id}/transactions")
public class TransactionController {

	@Autowired
	private TransactionService transactionService;

	@GetMapping
	public ResponseEntity<Page<TransactionResponse>> getMyAccountTransactions(@PathVariable("id") Long accountId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(transactionService.getMyAccountTransactions(accountId, page, size));
	}

	@PostMapping
	public ResponseEntity<TransactionResponse> createTransaction(@PathVariable("id") Long accountId,
			@RequestBody CreateTransactionRequest request) {

		TransactionResponse response = transactionService.createTransaction(accountId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}