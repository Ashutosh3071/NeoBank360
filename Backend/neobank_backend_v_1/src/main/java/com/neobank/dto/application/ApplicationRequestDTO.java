// src/main/java/com/neobank/dto/application/ApplicationRequestDTO.java

package com.neobank.dto.application;

import com.neobank.entity.Application.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRequestDTO {

    // ── OTP (verified by backend) ──
    private String otp;

    // ── Step 1: Customer Details ──
    private String accountType;
    private String fullName;
    private String fatherName;
    private String motherName;
    private String dateOfBirth;       // "yyyy-MM-dd" string → parsed in service
    private String gender;
    private String maritalStatus;
    private String nationality;
    private String occupation;
    private String annualIncome;
    private String phoneNumber;
    private String emailId;
    private String aadhaarNumber;
    private String panNumber;

    // ── Step 2: Current Address ──
    private String currentAddressLine;
    private String currentCity;
    private String currentState;
    private String currentPincode;

    // ── Step 2: Permanent Address ──
    private String permanentAddressLine;
    private String permanentCity;
    private String permanentState;
    private String permanentPincode;

    // ── Step 3: Nominee ──
    private String nomineeName;
    private String nomineeRelation;
    private String nomineeAge;
    private String nomineeMobileNumber;
    private String nomineeAddress;

    // ── Step 4: Documents (MultipartFile) ──
    private MultipartFile aadhaarCardFile;
    private MultipartFile panCardFile;
    private MultipartFile passportFile;         // optional
    private MultipartFile voterIdFile;          // optional
    private MultipartFile profilePhoto;
    private MultipartFile signatureImage;
    private MultipartFile addressProofDocument;
}