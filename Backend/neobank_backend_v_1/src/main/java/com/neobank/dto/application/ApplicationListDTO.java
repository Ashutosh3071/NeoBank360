// src/main/java/com/neobank/dto/application/ApplicationListDTO.java

package com.neobank.dto.application;

import com.neobank.entity.Application.ApplicationStatus;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationListDTO {
    private String      applicationId;
    private String      fullName;
    private String      emailId;
    private String      phoneNumber;
    private String      accountType;
    private String      gender;
    private String      occupation;
    private String      currentCity;
    private String      currentState;
    private ApplicationStatus status;
    private String      submittedOn;
    private String      aadhaarNumber;  // masked
    private String      panNumber;
}