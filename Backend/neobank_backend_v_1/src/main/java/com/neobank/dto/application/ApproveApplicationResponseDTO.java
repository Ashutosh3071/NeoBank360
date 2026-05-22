// src/main/java/com/neobank/dto/application/ApproveApplicationResponseDTO.java

package com.neobank.dto.application;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApproveApplicationResponseDTO {
    private String applicationId;
    private String status;
    private String userId;
    private String accountNumber;
    private String fullName;
    private String emailId;
    private String message;
}