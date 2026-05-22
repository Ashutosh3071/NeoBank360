// src/main/java/com/neobank/dto/application/ApplicationStatusResponseDTO.java

package com.neobank.dto.application;

import com.neobank.entity.Application.ApplicationStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusResponseDTO {

    // ── Application ──
    private String applicationId;
    private String status;
    private String submittedOn;

    // ── Step 1: Customer ──
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
    private String aadhaarNumber;   // masked: ••••••••1234
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
}