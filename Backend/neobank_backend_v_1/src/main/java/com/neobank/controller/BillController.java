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
import com.neobank.service.BillService;
import com.neobank.service.BudgetService;

import lombok.RequiredArgsConstructor;

//BillController.java
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BillController {

 private final BillService billService;

 @PostMapping
 public ResponseEntity<ApiResponseDTO<BillResponseDTO>> create(
         @RequestBody BillRequestDTO req, Authentication auth) {
     var result = billService.createBill(req, auth.getName());
     return ResponseEntity.status(HttpStatus.CREATED)
             .body(ApiResponseDTO.success("Bill created", result));
 }

 @GetMapping
 public ResponseEntity<ApiResponseDTO<List<BillResponseDTO>>> getAll(
         Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Bills fetched",
             billService.getMyBills(auth.getName())));
 }

 @GetMapping("/{id}")
 public ResponseEntity<ApiResponseDTO<BillResponseDTO>> getOne(
         @PathVariable Long id, Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Bill fetched",
             billService.getBill(id, auth.getName())));
 }

 @PatchMapping("/{id}/status")
 public ResponseEntity<ApiResponseDTO<BillResponseDTO>> updateStatus(
         @PathVariable Long id,
         @RequestBody BillStatusUpdateDTO req,
         Authentication auth) {
     return ResponseEntity.ok(ApiResponseDTO.success("Status updated",
             billService.updateStatus(id, req, auth.getName())));
 }

 @DeleteMapping("/{id}")
 public ResponseEntity<ApiResponseDTO<Void>> delete(
         @PathVariable Long id, Authentication auth) {
     billService.deleteBill(id, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("Bill deleted", null));
 }
}
