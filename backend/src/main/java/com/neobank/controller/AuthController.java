package com.neobank.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.neobank.dto.LoginRequest;
import com.neobank.dto.RegisterRequest;
import com.neobank.dto.UpdateProfileRequest;
import com.neobank.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private AuthService authService;

	/**
	 * Customer Registration Account will be created with status = PENDING_APPROVAL
	 */
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
		authService.register(request);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(Map.of("message", "Registration successful. You can now login."));
	}

	/**
	 * Login Only APPROVED users are allowed
	 */
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/**
	 * Get my profile
	 */
	@GetMapping("/me")
	public ResponseEntity<?> getMyProfile(Authentication authentication) {
		String email = authentication.getName();
		return ResponseEntity.ok(authService.getMyProfile(email));
	}

	/**
	 * Update my profile
	 */
	@PutMapping("/me")
	public ResponseEntity<?> updateMyProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {

		String email = authentication.getName();
		return ResponseEntity.ok(authService.updateMyProfile(email, request));
	}
}