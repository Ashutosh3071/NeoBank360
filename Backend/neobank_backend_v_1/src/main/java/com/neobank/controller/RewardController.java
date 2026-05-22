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
import com.neobank.dto.bill.BillRequestDTO;
import com.neobank.dto.bill.BillResponseDTO;
import com.neobank.dto.bill.BillStatusUpdateDTO;
import com.neobank.dto.budget.BudgetRequestDTO;
import com.neobank.dto.budget.BudgetResponseDTO;
import com.neobank.dto.budget.BudgetSummaryResponseDTO;
import com.neobank.dto.reward.RewardDTO;
import com.neobank.dto.reward.RewardHistoryDTO;
import com.neobank.service.BillService;
import com.neobank.service.BudgetService;
import com.neobank.service.RewardService;

import lombok.RequiredArgsConstructor;

//BillController.java
//RewardController.java
@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RewardController {

 private final RewardService rewardService;

 @GetMapping("/my")
 public ResponseEntity<ApiResponseDTO<RewardDTO>> getMyBalance(
         Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Balance fetched",
             rewardService.getMyBalance(auth.getName())));
 }

 @GetMapping("/{userId}")
 public ResponseEntity<ApiResponseDTO<RewardDTO>> getBalance(
         @PathVariable Long userId, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Balance fetched",
             rewardService.getBalance(userId, auth.getName())));
 }

 @GetMapping("/history")
 public ResponseEntity<ApiResponseDTO<List<RewardHistoryDTO>>> getHistory(
         @RequestParam(defaultValue = "0")  int page,
         @RequestParam(defaultValue = "20") int size,
         Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("History fetched",
             rewardService.getHistory(auth.getName(), page, size)));
 }
}