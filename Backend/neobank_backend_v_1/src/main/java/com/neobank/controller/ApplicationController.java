// src/main/java/com/neobank/controller/ApplicationController.java

package com.neobank.controller;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.application.ApplicationDetailDTO;
import com.neobank.dto.application.ApplicationListDTO;
import com.neobank.dto.application.ApplicationRequestDTO;
import com.neobank.dto.application.ApplicationResponseDTO;
import com.neobank.dto.application.ApplicationStatusResponseDTO;
import com.neobank.dto.application.ApproveApplicationRequestDTO;
import com.neobank.dto.application.ApproveApplicationResponseDTO;
import com.neobank.dto.application.SendOtpRequestDTO;
import com.neobank.dto.application.StatusOtpRequestDTO;
import com.neobank.dto.application.StatusVerifyRequestDTO;
import com.neobank.entity.Application.ApplicationStatus;
import com.neobank.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/application")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
public class ApplicationController {

	private final ApplicationService applicationService;

	// ─────────────────────────────────────────────
	// POST /api/application/send-otp
	// Body: { "email": "user@example.com" }
	// ─────────────────────────────────────────────

	@PostMapping("/send-otp")
	public ResponseEntity<ApiResponseDTO<Void>> sendOtp(@RequestBody SendOtpRequestDTO request) {
		log.info("Send OTP request for email: {}", request.getEmail());

		applicationService.sendOtp(request.getEmail());

		return ResponseEntity.ok(ApiResponseDTO.success("OTP sent successfully to " + request.getEmail(), null));
	}

	// ─────────────────────────────────────────────
	// POST /api/application/submit
	// Content-Type: multipart/form-data
	// All fields + files sent together
	// ─────────────────────────────────────────────
	
	// Add to ApplicationController.java

	// ── POST /api/application/submit-auth (JWT authenticated, no OTP)
	@PostMapping(value = "/submit-auth", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponseDTO<ApplicationResponseDTO>> submitApplicationAuthenticated(
	        @RequestParam("accountType")       String accountType,
	        @RequestParam("fullName")          String fullName,
	        @RequestParam("fatherName")        String fatherName,
	        @RequestParam("motherName")        String motherName,
	        @RequestParam("dateOfBirth")       String dateOfBirth,
	        @RequestParam("gender")            String gender,
	        @RequestParam(value="maritalStatus",  required=false) String maritalStatus,
	        @RequestParam(value="nationality",    required=false) String nationality,
	        @RequestParam("occupation")        String occupation,
	        @RequestParam(value="annualIncome",   required=false) String annualIncome,
	        @RequestParam("phoneNumber")       String phoneNumber,
	        @RequestParam("emailId")           String emailId,
	        @RequestParam("aadhaarNumber")     String aadhaarNumber,
	        @RequestParam("panNumber")         String panNumber,
	        @RequestParam("currentAddressLine")    String currentAddressLine,
	        @RequestParam("currentCity")           String currentCity,
	        @RequestParam("currentState")          String currentState,
	        @RequestParam("currentPincode")        String currentPincode,
	        @RequestParam("permanentAddressLine")  String permanentAddressLine,
	        @RequestParam("permanentCity")         String permanentCity,
	        @RequestParam("permanentState")        String permanentState,
	        @RequestParam("permanentPincode")      String permanentPincode,
	        @RequestParam("nomineeName")           String nomineeName,
	        @RequestParam("nomineeRelation")       String nomineeRelation,
	        @RequestParam("nomineeAge")            String nomineeAge,
	        @RequestParam("nomineeMobileNumber")   String nomineeMobileNumber,
	        @RequestParam("nomineeAddress")        String nomineeAddress,
	        @RequestParam("aadhaarCardFile")       MultipartFile aadhaarCardFile,
	        @RequestParam("panCardFile")           MultipartFile panCardFile,
	        @RequestParam(value="passportFile",    required=false) MultipartFile passportFile,
	        @RequestParam(value="voterIdFile",     required=false) MultipartFile voterIdFile,
	        @RequestParam("profilePhoto")          MultipartFile profilePhoto,
	        @RequestParam("signatureImage")        MultipartFile signatureImage,
	        @RequestParam("addressProofDocument")  MultipartFile addressProofDocument,
	        Authentication auth
	) throws Exception {

	    log.info("Authenticated application submit from: {}", auth.getName());

	    ApplicationRequestDTO dto = ApplicationRequestDTO.builder()
	            .accountType(accountType)
	            .fullName(fullName)
	            .fatherName(fatherName)
	            .motherName(motherName)
	            .dateOfBirth(dateOfBirth)
	            .gender(gender)
	            .maritalStatus(maritalStatus)
	            .nationality(nationality)
	            .occupation(occupation)
	            .annualIncome(annualIncome)
	            .phoneNumber(phoneNumber)
	            .emailId(emailId)
	            .aadhaarNumber(aadhaarNumber)
	            .panNumber(panNumber)
	            .currentAddressLine(currentAddressLine)
	            .currentCity(currentCity)
	            .currentState(currentState)
	            .currentPincode(currentPincode)
	            .permanentAddressLine(permanentAddressLine)
	            .permanentCity(permanentCity)
	            .permanentState(permanentState)
	            .permanentPincode(permanentPincode)
	            .nomineeName(nomineeName)
	            .nomineeRelation(nomineeRelation)
	            .nomineeAge(nomineeAge)
	            .nomineeMobileNumber(nomineeMobileNumber)
	            .nomineeAddress(nomineeAddress)
	            .aadhaarCardFile(aadhaarCardFile)
	            .panCardFile(panCardFile)
	            .passportFile(passportFile)
	            .voterIdFile(voterIdFile)
	            .profilePhoto(profilePhoto)
	            .signatureImage(signatureImage)
	            .addressProofDocument(addressProofDocument)
	            .build();

	    // ── No OTP needed — user is authenticated via JWT ──
	    ApplicationResponseDTO response = applicationService.submitApplicationAuthenticated(dto, auth.getName());
	    return ResponseEntity.ok(ApiResponseDTO.success("Application submitted successfully", response));
	}

	@PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponseDTO<ApplicationResponseDTO>> submitApplication(

			// ── OTP ──
			@RequestParam("otp") String otp,

			// ── Step 1: Customer ──
			@RequestParam("accountType") String accountType, @RequestParam("fullName") String fullName,
			@RequestParam("fatherName") String fatherName, @RequestParam("motherName") String motherName,
			@RequestParam("dateOfBirth") String dateOfBirth, @RequestParam("gender") String gender,
			@RequestParam(value = "maritalStatus", required = false) String maritalStatus,
			@RequestParam(value = "nationality", required = false) String nationality,
			@RequestParam("occupation") String occupation,
			@RequestParam(value = "annualIncome", required = false) String annualIncome,
			@RequestParam("phoneNumber") String phoneNumber, @RequestParam("emailId") String emailId,
			@RequestParam("aadhaarNumber") String aadhaarNumber, @RequestParam("panNumber") String panNumber,

			// ── Step 2: Current Address ──
			@RequestParam("currentAddressLine") String currentAddressLine,
			@RequestParam("currentCity") String currentCity, @RequestParam("currentState") String currentState,
			@RequestParam("currentPincode") String currentPincode,

			// ── Step 2: Permanent Address ──
			@RequestParam("permanentAddressLine") String permanentAddressLine,
			@RequestParam("permanentCity") String permanentCity, @RequestParam("permanentState") String permanentState,
			@RequestParam("permanentPincode") String permanentPincode,

			// ── Step 3: Nominee ──
			@RequestParam("nomineeName") String nomineeName, @RequestParam("nomineeRelation") String nomineeRelation,
			@RequestParam("nomineeAge") String nomineeAge,
			@RequestParam("nomineeMobileNumber") String nomineeMobileNumber,
			@RequestParam("nomineeAddress") String nomineeAddress,

			// ── Step 4: Documents ──
			@RequestParam("aadhaarCardFile") MultipartFile aadhaarCardFile,
			@RequestParam("panCardFile") MultipartFile panCardFile,
			@RequestParam(value = "passportFile", required = false) MultipartFile passportFile,
			@RequestParam(value = "voterIdFile", required = false) MultipartFile voterIdFile,
			@RequestParam("profilePhoto") MultipartFile profilePhoto,
			@RequestParam("signatureImage") MultipartFile signatureImage,
			@RequestParam("addressProofDocument") MultipartFile addressProofDocument

	) throws Exception {

		log.info("Application submit request from email: {}", emailId);

		// Build DTO from request params
		ApplicationRequestDTO dto = ApplicationRequestDTO.builder().otp(otp).accountType(accountType).fullName(fullName)
				.fatherName(fatherName).motherName(motherName).dateOfBirth(dateOfBirth).gender(gender)
				.maritalStatus(maritalStatus).nationality(nationality).occupation(occupation).annualIncome(annualIncome)
				.phoneNumber(phoneNumber).emailId(emailId).aadhaarNumber(aadhaarNumber).panNumber(panNumber)
				.currentAddressLine(currentAddressLine).currentCity(currentCity).currentState(currentState)
				.currentPincode(currentPincode).permanentAddressLine(permanentAddressLine).permanentCity(permanentCity)
				.permanentState(permanentState).permanentPincode(permanentPincode).nomineeName(nomineeName)
				.nomineeRelation(nomineeRelation).nomineeAge(nomineeAge).nomineeMobileNumber(nomineeMobileNumber)
				.nomineeAddress(nomineeAddress).aadhaarCardFile(aadhaarCardFile).panCardFile(panCardFile)
				.passportFile(passportFile).voterIdFile(voterIdFile).profilePhoto(profilePhoto)
				.signatureImage(signatureImage).addressProofDocument(addressProofDocument).build();

		ApplicationResponseDTO response = applicationService.submitApplication(dto);

		return ResponseEntity.ok(ApiResponseDTO.success("Application submitted successfully", response));
	}

	// ── POST /api/application/status/send-otp ──
	@PostMapping("/status/send-otp")
	public ResponseEntity<ApiResponseDTO<Void>> sendStatusOtp(@RequestBody StatusOtpRequestDTO request) {
		applicationService.sendStatusOtp(request.getQuery());
		return ResponseEntity.ok(ApiResponseDTO.success("OTP sent to registered email", null));
	}

	// ── POST /api/application/status/verify ──
	@PostMapping("/status/verify")
	public ResponseEntity<ApiResponseDTO<ApplicationStatusResponseDTO>> verifyStatusOtp(
			@RequestBody StatusVerifyRequestDTO request) {
		ApplicationStatusResponseDTO result = applicationService.verifyStatusOtpAndGetDetails(request.getQuery(),
				request.getOtp());
		return ResponseEntity.ok(ApiResponseDTO.success("Application found", result));
	}

	// Add these endpoints to ApplicationController.java

	// ── GET /api/application/all?status=SUBMITTED&page=0&size=10 ──
	@GetMapping("/all")
	public ResponseEntity<ApiResponseDTO<Page<ApplicationListDTO>>> getAllApplications(
			@RequestParam(required = false) ApplicationStatus status, Pageable pageable) {
		Page<ApplicationListDTO> result = applicationService.getAllApplications(status, pageable);
		return ResponseEntity.ok(ApiResponseDTO.success("Applications fetched", result));
	}

	// ── GET /api/application/{applicationId} ──
	@GetMapping("/{applicationId}")
	public ResponseEntity<ApiResponseDTO<ApplicationDetailDTO>> getApplicationById(@PathVariable String applicationId) {
		ApplicationDetailDTO result = applicationService.getApplicationById(applicationId);
		return ResponseEntity.ok(ApiResponseDTO.success("Application details", result));
	}

	// ── POST /api/application/approve ──
	@PostMapping("/approve")
	public ResponseEntity<ApiResponseDTO<ApproveApplicationResponseDTO>> approveApplication(
			@RequestBody ApproveApplicationRequestDTO req, Authentication auth) {
		log.info("/approve run");
		ApproveApplicationResponseDTO result = applicationService.approveApplication(req, auth.getName());
		return ResponseEntity.ok(ApiResponseDTO.success(result.getMessage(), result));
	}
}