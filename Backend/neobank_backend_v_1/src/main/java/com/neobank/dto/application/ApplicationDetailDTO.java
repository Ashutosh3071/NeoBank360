// src/main/java/com/neobank/dto/application/ApplicationDetailDTO.java

package com.neobank.dto.application;

import com.neobank.entity.Application.ApplicationStatus;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationDetailDTO {

    // Basic
    private String            applicationId;
    private ApplicationStatus status;
    private String            submittedOn;
    private String            approvedBy;
    private String            approvedAt;
    private String            rejectionReason;

    // Personal
    private String accountType;
    private String fullName;
    private String fatherName;
    private String motherName;
    private String dateOfBirth;
    private String gender;
    private String maritalStatus;
    private String nationality;
    private String occupation;
    private String annualIncome;
    private String phoneNumber;
    private String emailId;
    private String aadhaarNumber;
    private String panNumber;

    // Current Address
    private String currentAddressLine;
    private String currentCity;
    private String currentState;
    private String currentPincode;

    // Permanent Address
    private String permanentAddressLine;
    private String permanentCity;
    private String permanentState;
    private String permanentPincode;

    // Nominee
    private String nomineeName;
    private String nomineeRelation;
    private String nomineeAge;
    private String nomineeMobileNumber;
    private String nomineeAddress;

    // Documents (base64 for frontend preview)
    private String aadhaarCardFileType;
    private String aadhaarCardFileBase64;

    private String panCardFileType;
    private String panCardFileBase64;

    private String profilePhotoType;
    private String profilePhotoBase64;

    private String signatureImageType;
    private String signatureImageBase64;

    private String addressProofDocumentType;
    private String addressProofDocumentBase64;

    private String passportFileType;
    private String passportFileBase64;

    private String voterIdFileType;
    private String voterIdFileBase64;
}