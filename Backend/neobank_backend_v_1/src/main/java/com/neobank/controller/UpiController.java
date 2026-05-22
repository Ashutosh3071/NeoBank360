// src/main/java/com/neobank/controller/AccountController.java

package com.neobank.controller;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.account.*;
import com.neobank.dto.upi.ChangeUpiPinRequestDTO;
import com.neobank.dto.upi.CreateUpiRequestDTO;
import com.neobank.dto.upi.SetUpiPinRequestDTO;
import com.neobank.dto.upi.UpiIdResponseDTO;
import com.neobank.dto.upi.UpiPayRequestDTO;
import com.neobank.dto.upi.UpiTransactionResponseDTO;
import com.neobank.dto.upi.VpaLookupResponseDTO;
import com.neobank.entity.Account.AccountStatus;
import com.neobank.service.AccountService;
import com.neobank.service.UpiService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//UpiController.java
@RestController
@RequestMapping("/api/upi")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UpiController {

 private final UpiService upiService;

 @PostMapping("/create")
 public ResponseEntity<ApiResponseDTO<UpiIdResponseDTO>> create(
         @RequestBody CreateUpiRequestDTO req, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("UPI ID created",
             upiService.createUpiId(req, auth.getName())));
 }

 @GetMapping("/my")
 public ResponseEntity<ApiResponseDTO<List<UpiIdResponseDTO>>> getAllMy(Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("UPI IDs fetched",
             upiService.getAllMyUpiIds(auth.getName())));
 }

 @GetMapping("/account/{accountNumber}")
 public ResponseEntity<ApiResponseDTO<List<UpiIdResponseDTO>>> getByAccount(
         @PathVariable String accountNumber, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("UPI IDs fetched",
             upiService.getMyUpiIds(accountNumber, auth.getName())));
 }

 @PostMapping("/pin/send-otp")
 public ResponseEntity<ApiResponseDTO<Void>> sendPinOtp(
         @RequestParam String vpa, Authentication auth) {
     upiService.sendPinSetupOtp(vpa, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("OTP sent", null));
 }

 @PostMapping("/pin/set")
 public ResponseEntity<ApiResponseDTO<Void>> setPin(
         @RequestBody SetUpiPinRequestDTO req, Authentication auth) {
     upiService.setUpiPin(req, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("PIN set successfully", null));
 }

 @PostMapping("/pin/change")
 public ResponseEntity<ApiResponseDTO<Void>> changePin(
         @RequestBody ChangeUpiPinRequestDTO req, Authentication auth) {
     upiService.changeUpiPin(req, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("PIN changed", null));
 }

 @GetMapping("/lookup")
 public ResponseEntity<ApiResponseDTO<VpaLookupResponseDTO>> lookup(
         @RequestParam String vpa) {
     return ResponseEntity.ok(ApiResponseDTO.success("VPA lookup",
             upiService.lookupVpa(vpa)));
 }

 @PostMapping("/pay")
 public ResponseEntity<ApiResponseDTO<UpiTransactionResponseDTO>> pay(
         @RequestBody UpiPayRequestDTO req, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Payment successful",
             upiService.pay(req, auth.getName())));
 }

 @GetMapping("/transactions/{vpa}")
 public ResponseEntity<ApiResponseDTO<Page<UpiTransactionResponseDTO>>> transactions(
         @PathVariable String vpa,
         @RequestParam(defaultValue = "0")  int page,
         @RequestParam(defaultValue = "15") int size,
         Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Transactions fetched",
             upiService.getUpiTransactions(vpa, auth.getName(),
                     PageRequest.of(page, size))));
 }

 @PostMapping("/set-primary")
 public ResponseEntity<ApiResponseDTO<Void>> setPrimary(
         @RequestParam String vpa, Authentication auth) {
     upiService.setPrimary(vpa, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("Primary UPI ID updated", null));
 }

 @PostMapping("/block")
 public ResponseEntity<ApiResponseDTO<Void>> block(
         @RequestParam String vpa, Authentication auth) {
     upiService.blockUpiId(vpa, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("UPI ID blocked", null));
 }

 @DeleteMapping("/delete")
 public ResponseEntity<ApiResponseDTO<Void>> delete(
         @RequestParam String vpa, Authentication auth) {
     upiService.deleteUpiId(vpa, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("UPI ID deleted", null));
 }

 @PutMapping("/limits")
 public ResponseEntity<ApiResponseDTO<UpiIdResponseDTO>> updateLimits(
         @RequestParam String vpa,
         @RequestParam(required = false) Long dailyLimit,
         @RequestParam(required = false) Long perTxnLimit,
         Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Limits updated",
             upiService.updateLimits(vpa, dailyLimit, perTxnLimit, auth.getName())));
 }
}