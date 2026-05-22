package com.neobank.service;

import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neobank.dto.user.UserResponseDTO;
import com.neobank.dto.user.UserStatusUpdateDTO;
import com.neobank.entity.User;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.AccountRepository;
import com.neobank.repository.TransactionRepository;
import com.neobank.repository.UserRepository;
import com.neobank.util.TransactionReferenceGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {


private final UserRepository   userRepository;
	
	// UserService.java - key methods

	public Page<UserResponseDTO> getAllUsers(User.UserStatus status, Pageable pageable) {
	    Page<User> page = (status != null)
	            ? userRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
	            : userRepository.findAll(pageable);
	    return page.map(this::buildResponse);
	}

	@Transactional
	public void updateUserStatus(UserStatusUpdateDTO req, String adminUsername) {
	    User user = userRepository.findById(req.getUserId())
	            .orElseThrow(() -> new NeoBankException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
	    user.setStatus(User.UserStatus.valueOf(req.getStatus()));
	    user.setUpdatedBy(adminUsername);
	    if ("ACTIVE".equals(req.getStatus())) {
	        user.setIsLocked(false);
	        user.setFailedLoginAttempts(0);
	    }
	    userRepository.save(user);
	}

	private UserResponseDTO buildResponse(User u) {
	    return UserResponseDTO.builder()
	            .id(u.getId())
	            .username(u.getUsername())
	            .email(u.getEmail())
	            .fullName(u.getFullName())
	            .phone(u.getPhone())
	            .role(u.getRole().name())
	            .status(u.getStatus().name())
	            .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : null)
	            .lastLoginAt(u.getLastLoginAt() != null ? u.getLastLoginAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : null)
	            .build();
	}
}
