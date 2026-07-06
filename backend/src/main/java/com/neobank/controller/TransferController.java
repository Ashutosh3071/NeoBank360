package com.neobank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.TransferRequest;
import com.neobank.dto.TransferResponse;
import com.neobank.service.TransferService;

@RestController
@RequestMapping("/transfers")
public class TransferController {

	@Autowired
	private TransferService transferService;

	@PostMapping
	public ResponseEntity<TransferResponse> transferMoney( @RequestBody TransferRequest request) {
		TransferResponse response = transferService.transferMoney(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}