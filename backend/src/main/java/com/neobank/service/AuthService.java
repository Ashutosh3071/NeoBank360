package com.neobank.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.neobank.dto.LoginRequest;
import com.neobank.dto.RegisterRequest;
import com.neobank.dto.UpdateProfileRequest;
import com.neobank.entity.User;
import com.neobank.enums.Role;
import com.neobank.repository.UserRepository;
import com.neobank.security.JwtUtil;

@Service
public class AuthService {

	private static final Map<Long, List<LocalDateTime>> loginEventsCache = new ConcurrentHashMap<>();

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private AuthenticationManager authenticationManager;

	/**
	 * ✅ USER REGISTRATION User is ACTIVE immediately (no admin approval)
	 */
	@Transactional
	public User register(RegisterRequest request) {

		if (request.getEmail() == null || request.getEmail().trim().isEmpty() ||
				request.getFullName() == null || request.getFullName().trim().isEmpty() ||
				request.getPassword() == null || request.getPassword().isEmpty() ||
				request.getAadhaarNumber() == null || request.getAadhaarNumber().trim().isEmpty() ||
				request.getPanNumber() == null || request.getPanNumber().trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All fields are required: fullName, email, password, aadhaarNumber, panNumber");
		}

		String normalizedEmail = request.getEmail().trim().toLowerCase();
		if (!normalizedEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
		}

		String password = request.getPassword();
		if (password.length() < 8) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
		}
		if (!password.matches(".*[A-Z].*")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one uppercase letter");
		}
		if (!password.matches(".*[a-z].*")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one lowercase letter");
		}
		if (!password.matches(".*\\d.*")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one digit");
		}
		if (password.matches("^[A-Za-z0-9]*$")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one special character");
		}
		String normalizedAadhaar = request.getAadhaarNumber().replaceAll("\\s+", "");
		String normalizedPan = request.getPanNumber().trim().toUpperCase();

		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		if (userRepository.existsByAadhaarNumber(normalizedAadhaar)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Aadhaar number already exists");
		}

		if (userRepository.existsByPanNumber(normalizedPan)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "PAN number already exists");
		}

		User user = new User();
		user.setFullName(request.getFullName().trim());
		user.setEmail(normalizedEmail);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setAadhaarNumber(normalizedAadhaar);
		user.setPanNumber(normalizedPan);
		user.setRole(Role.CUSTOMER);

		// ✅ user active immediately
		user.setIsActive(true);

		return userRepository.save(user);
	}

	/**
	 * ✅ LOGIN Only inactive users are blocked
	 */
	public Map<String, Object> login(LoginRequest request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

		User user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		if (!Boolean.TRUE.equals(user.getIsActive())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is inactive");
		}

		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));
		} catch (org.springframework.security.core.AuthenticationException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}

		String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

		// Track successful login event
		List<LocalDateTime> events = loginEventsCache.computeIfAbsent(user.getId(), k -> new CopyOnWriteArrayList<>());
		events.add(0, LocalDateTime.now());
		if (events.size() > 5) {
			events.remove(events.size() - 1);
		}

		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("userId", user.getId());
		response.put("email", user.getEmail());
		response.put("role", user.getRole().name());
		response.put("message", "Login successful");

		return response;
	}

	public List<LocalDateTime> getLoginEvents(Long userId) {
		return loginEventsCache.getOrDefault(userId, Collections.emptyList());
	}

	/**
	 * ✅ GET MY PROFILE
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getMyProfile(String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		Map<String, Object> profile = new LinkedHashMap<>();
		profile.put("id", user.getId());
		profile.put("fullName", user.getFullName());
		profile.put("email", user.getEmail());
		profile.put("role", user.getRole());
		profile.put("isActive", user.getIsActive());
		profile.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

		return profile;
	}

	/**
	 * ✅ UPDATE PROFILE
	 */
	@Transactional
	public Map<String, Object> updateMyProfile(String email, UpdateProfileRequest request) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (request.getFullName() == null || request.getFullName().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
		}

		user.setFullName(request.getFullName().trim());
		userRepository.save(user);

		Map<String, Object> profile = new LinkedHashMap<>();
		profile.put("id", user.getId());
		profile.put("fullName", user.getFullName());
		profile.put("email", user.getEmail());
		profile.put("role", user.getRole());
		profile.put("isActive", user.getIsActive());
		profile.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

		return profile;
	}
}