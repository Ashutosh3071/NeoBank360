// src/main/java/com/neobank/controller/AccountController.java

package com.neobank.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.accountrequest.AccountRequestDTO;
import com.neobank.dto.accountrequest.AccountRequestResponseDTO;
import com.neobank.dto.accountrequest.ReviewAccountRequestDTO;
import com.neobank.entity.AccountRequest;
import com.neobank.service.AccountRequestService;

import lombok.RequiredArgsConstructor;


//AccountRequestController.java
@RestController
@RequestMapping("/api/account-request")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountRequestController {

 private final AccountRequestService accountRequestService;

 // Customer
 @PostMapping("/submit")
 public ResponseEntity<ApiResponseDTO<AccountRequestResponseDTO>> submit(
         @RequestBody AccountRequestDTO req, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Request submitted",
             accountRequestService.submitRequest(req, auth.getName())));
 }

 @GetMapping("/my")
 public ResponseEntity<ApiResponseDTO<Page<AccountRequestResponseDTO>>> myRequests(
         @RequestParam(defaultValue = "0")  int page,
         @RequestParam(defaultValue = "10") int size,
         Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Requests fetched",
             accountRequestService.getMyRequests(auth.getName(),
                     PageRequest.of(page, size))));
 }

 // Admin
 @GetMapping("/admin/all")
 public ResponseEntity<ApiResponseDTO<Page<AccountRequestResponseDTO>>> allRequests(
         @RequestParam(required = false) AccountRequest.RequestStatus status,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "10") int size) {
     return ResponseEntity.ok(ApiResponseDTO.success("Requests fetched",
             accountRequestService.getAllRequests(status, PageRequest.of(page, size))));
 }

 @GetMapping("/admin/{requestId}")
 public ResponseEntity<ApiResponseDTO<AccountRequestResponseDTO>> getDetail(
         @PathVariable String requestId) {
     return ResponseEntity.ok(ApiResponseDTO.success("Detail fetched",
             accountRequestService.getRequestDetail(requestId)));
 }

 @PostMapping("/admin/approve")
 public ResponseEntity<ApiResponseDTO<AccountRequestResponseDTO>> approve(
         @RequestBody ReviewAccountRequestDTO req, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Request approved",
             accountRequestService.approveRequest(req, auth.getName())));
 }

 @PostMapping("/admin/reject")
 public ResponseEntity<ApiResponseDTO<AccountRequestResponseDTO>> reject(
         @RequestBody ReviewAccountRequestDTO req, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Request rejected",
             accountRequestService.rejectRequest(req, auth.getName())));
 }
}