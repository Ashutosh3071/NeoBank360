// src/main/java/com/neobank/controller/AccountController.java

package com.neobank.controller;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.account.*;
import com.neobank.dto.user.UserResponseDTO;
import com.neobank.dto.user.UserStatusUpdateDTO;
import com.neobank.entity.Account.AccountStatus;
import com.neobank.entity.User;
import com.neobank.service.AccountService;
import com.neobank.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//src/main/java/com/neobank/controller/UserController.java

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

 private final UserService userService;

 @GetMapping("/admin/all")
 public ResponseEntity<ApiResponseDTO<Page<UserResponseDTO>>> getAllUsers(
         @RequestParam(required = false) User.UserStatus status,
         @RequestParam(defaultValue = "0")  int page,
         @RequestParam(defaultValue = "10") int size) {
     Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
     return ResponseEntity.ok(ApiResponseDTO.success("Users fetched",
             userService.getAllUsers(status, pageable)));
 }

 @PutMapping("/admin/status")
 public ResponseEntity<ApiResponseDTO<Void>> updateStatus(
         @RequestBody UserStatusUpdateDTO req,
         Authentication auth) {
     userService.updateUserStatus(req, auth.getName());
     return ResponseEntity.ok(ApiResponseDTO.success("Status updated", null));
 }
}