// src/main/java/com/neobank/entity/Application.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "applications", indexes = { @Index(name = "idx_email", columnList = "email_id"),
		@Index(name = "idx_phone", columnList = "phone_number"), @Index(name = "idx_status", columnList = "status"),
		@Index(name = "idx_app_id", columnList = "application_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

	// ─────────────────────────────────────────────
	// PRIMARY KEY
	// ─────────────────────────────────────────────

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "application_id", unique = true, nullable = false, length = 20)
	private String applicationId; // e.g. NB20260000001

	// ─────────────────────────────────────────────
	// STEP 1 — CUSTOMER DETAILS
	// ─────────────────────────────────────────────

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false, length = 10)
	private AccountType accountType;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(name = "father_name", nullable = false, length = 100)
	private String fatherName;

	@Column(name = "mother_name", nullable = false, length = 100)
	private String motherName;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender", nullable = false, length = 10)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(name = "marital_status", length = 10)
	private MaritalStatus maritalStatus;

	@Column(name = "nationality", length = 50)
	private String nationality;

	@Column(name = "occupation", nullable = false, length = 100)
	private String occupation;

	@Column(name = "annual_income", length = 20)
	private String annualIncome;

	@Column(name = "phone_number", nullable = false, length = 10)
	private String phoneNumber;

	@Column(name = "email_id", nullable = false, length = 150)
	private String emailId;

	@Column(name = "aadhaar_number", nullable = false, length = 12)
	private String aadhaarNumber;

	@Column(name = "pan_number", nullable = false, length = 10)
	private String panNumber;

	// ─────────────────────────────────────────────
	// STEP 2 — CURRENT ADDRESS
	// ─────────────────────────────────────────────

	@Column(name = "current_address_line", nullable = false, length = 255)
	private String currentAddressLine;

	@Column(name = "current_city", nullable = false, length = 100)
	private String currentCity;

	@Column(name = "current_state", nullable = false, length = 100)
	private String currentState;

	@Column(name = "current_pincode", nullable = false, length = 6)
	private String currentPincode;

	// ─────────────────────────────────────────────
	// STEP 2 — PERMANENT ADDRESS
	// ─────────────────────────────────────────────

	@Column(name = "permanent_address_line", nullable = false, length = 255)
	private String permanentAddressLine;

	@Column(name = "permanent_city", nullable = false, length = 100)
	private String permanentCity;

	@Column(name = "permanent_state", nullable = false, length = 100)
	private String permanentState;

	@Column(name = "permanent_pincode", nullable = false, length = 6)
	private String permanentPincode;

	// ─────────────────────────────────────────────
	// STEP 3 — NOMINEE DETAILS
	// ─────────────────────────────────────────────

	@Column(name = "nominee_name", nullable = false, length = 100)
	private String nomineeName;

	@Enumerated(EnumType.STRING)
	@Column(name = "nominee_relation", nullable = false, length = 10)
	private NomineeRelation nomineeRelation;

	@Column(name = "nominee_age", nullable = false)
	private Integer nomineeAge;

	@Column(name = "nominee_mobile_number", nullable = false, length = 10)
	private String nomineeMobileNumber;

	@Column(name = "nominee_address", nullable = false, length = 255)
	private String nomineeAddress;

	// ─────────────────────────────────────────────
	// STEP 4 — DOCUMENTS (BLOB)
	// Stored directly in DB as binary
	// ─────────────────────────────────────────────

	@Lob
	@Column(name = "aadhaar_card_file", nullable = false, columnDefinition = "LONGBLOB")
	private byte[] aadhaarCardFile;

	@Column(name = "aadhaar_card_file_name", nullable = false, length = 255)
	private String aadhaarCardFileName;

	@Column(name = "aadhaar_card_file_type", nullable = false, length = 50)
	private String aadhaarCardFileType;

	// ──

	@Lob
	@Column(name = "pan_card_file", nullable = false, columnDefinition = "LONGBLOB")
	private byte[] panCardFile;

	@Column(name = "pan_card_file_name", nullable = false, length = 255)
	private String panCardFileName;

	@Column(name = "pan_card_file_type", nullable = false, length = 50)
	private String panCardFileType;

	// ──

	@Lob
	@Column(name = "passport_file", columnDefinition = "LONGBLOB")
	private byte[] passportFile;

	@Column(name = "passport_file_name", length = 255)
	private String passportFileName;

	@Column(name = "passport_file_type", length = 50)
	private String passportFileType;

	// ──

	@Lob
	@Column(name = "voter_id_file", columnDefinition = "LONGBLOB")
	private byte[] voterIdFile;

	@Column(name = "voter_id_file_name", length = 255)
	private String voterIdFileName;

	@Column(name = "voter_id_file_type", length = 50)
	private String voterIdFileType;

	// ──

	@Lob
	@Column(name = "profile_photo", nullable = false, columnDefinition = "LONGBLOB")
	private byte[] profilePhoto;

	@Column(name = "profile_photo_name", nullable = false, length = 255)
	private String profilePhotoName;

	@Column(name = "profile_photo_type", nullable = false, length = 50)
	private String profilePhotoType;

	// ──

	@Lob
	@Column(name = "signature_image", nullable = false, columnDefinition = "LONGBLOB")
	private byte[] signatureImage;

	@Column(name = "signature_image_name", nullable = false, length = 255)
	private String signatureImageName;

	@Column(name = "signature_image_type", nullable = false, length = 50)
	private String signatureImageType;

	// ──

	@Lob
	@Column(name = "address_proof_document", nullable = false, columnDefinition = "LONGBLOB")
	private byte[] addressProofDocument;

	@Column(name = "address_proof_document_name", nullable = false, length = 255)
	private String addressProofDocumentName;

	@Column(name = "address_proof_document_type", nullable = false, length = 50)
	private String addressProofDocumentType;

	// ─────────────────────────────────────────────
	// OTP — Linked to OtpVerification entity
	// ─────────────────────────────────────────────

	@Column(name = "otp_verified", nullable = false)
	private Boolean otpVerified = false;

	// ─────────────────────────────────────────────
	// APPLICATION STATUS
	//
	// RULES:
	// - Only ONE active (PENDING/SUBMITTED/APPROVED)
	// application allowed per email/aadhaar/pan at a time
	// - If REJECTED → allowed to apply again
	// ─────────────────────────────────────────────

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ApplicationStatus status = ApplicationStatus.PENDING;

	// ─────────────────────────────────────────────
	// AUDIT
	// ─────────────────────────────────────────────

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// ─────────────────────────────────────────────
	// OTP RELATION
	// ─────────────────────────────────────────────

	@OneToMany(mappedBy = "application", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<OtpVerification> otpVerifications;

	// Add inside Application.java after status field

	// ─────────────────────────────────────────────
	// APPROVAL DETAILS
	// ─────────────────────────────────────────────

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by_user_id")
	private User approvedBy;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	// ─────────────────────────────────────────────
	// ENUMS
	// ─────────────────────────────────────────────

	public enum AccountType {
		SAVINGS, CURRENT, SALARY
	}

	public enum Gender {
		MALE, FEMALE, OTHER
	}

	public enum MaritalStatus {
		SINGLE, MARRIED, DIVORCED, WIDOWED
	}

	public enum NomineeRelation {
		SPOUSE, FATHER, MOTHER, SON, DAUGHTER, SIBLING, OTHER
	}

	public enum ApplicationStatus {
		PENDING, // Saved but OTP not yet verified
		SUBMITTED, // OTP verified — under bank review
		APPROVED, // Bank approved — account created
		REJECTED // Bank rejected — can apply again
	}
}