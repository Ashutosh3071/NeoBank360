package com.neobank.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.neobank.enums.Role;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Data
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@JsonIgnore
	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Enumerated(EnumType.STRING)
	private Role role;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "aadhaar_number", nullable = false, unique = true, length = 12)
	private String aadhaarNumber;

	@Column(name = "pan_number", nullable = false, unique = true, length = 10)
	private String panNumber;

	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.isActive = true;
	}
}