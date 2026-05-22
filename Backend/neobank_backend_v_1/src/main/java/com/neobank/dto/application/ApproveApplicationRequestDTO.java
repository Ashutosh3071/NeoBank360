// src/main/java/com/neobank/dto/application/ApproveApplicationRequestDTO.java

package com.neobank.dto.application;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ApproveApplicationRequestDTO {
    private String applicationId;
    private String action;           // "APPROVE" or "REJECT"
    private String rejectionReason;  // required only if REJECT
    private String branchName;       // for account creation
    private String branchCode;
    private String ifscCode;
}