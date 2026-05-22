// src/main/java/com/neobank/dto/application/ApplicationResponseDTO.java

package com.neobank.dto.application;

import com.neobank.entity.Application.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponseDTO {

    private String applicationId;
    private String fullName;
    private String accountType;
    private String phoneNumber;
    private String emailId;
    private String aadhaarNumber;   // masked: ••••••••1234
    private String panNumber;
    private ApplicationStatus status;
    private String submittedOn;     // formatted date string
}