// src/main/java/com/neobank/entity/User.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email",    columnList = "email",    unique = true),
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_phone",    columnList = "phone"),
        @Index(name = "idx_user_status",   columnList = "status"),
        @Index(name = "idx_user_role",     columnList = "role")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Credentials ──
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 10)
    private String phone;

    // ── Personal ──
    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "father_name", length = 100)
    private String fatherName;

    @Column(name = "mother_name", length = 100)
    private String motherName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 10)
    private MaritalStatus maritalStatus;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "annual_income", length = 20)
    private String annualIncome;

    // ── KYC ──
    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    // ── Current Address ──
    @Column(name = "current_address_line", length = 255)
    private String currentAddressLine;

    @Column(name = "current_city", length = 100)
    private String currentCity;

    @Column(name = "current_state", length = 100)
    private String currentState;

    @Column(name = "current_pincode", length = 6)
    private String currentPincode;

    // ── Permanent Address ──
    @Column(name = "permanent_address_line", length = 255)
    private String permanentAddressLine;

    @Column(name = "permanent_city", length = 100)
    private String permanentCity;

    @Column(name = "permanent_state", length = 100)
    private String permanentState;

    @Column(name = "permanent_pincode", length = 6)
    private String permanentPincode;

    // ── Accounts (one user → many accounts) ──
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts;

    // ── Application link ──
    @Column(name = "application_id", length = 20)
    private String applicationId;

    // ── Role & Status ──
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    // ── Security ──
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    // ── Profile Photo ──
    @Lob
    @Column(name = "profile_photo", columnDefinition = "LONGBLOB")
    private byte[] profilePhoto;

    @Column(name = "profile_photo_type", length = 50)
    private String profilePhotoType;

    // ── Audit ──
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // ── Enums ──
    public enum Role {
        CUSTOMER, MANAGER, ADMIN, SUPER_ADMIN
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, LOCKED
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum MaritalStatus {
        SINGLE, MARRIED, DIVORCED, WIDOWED
    }
}