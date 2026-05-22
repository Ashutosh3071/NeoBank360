package com.neobank.dto.accountrequest;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountRequestResponseDTO {
    private Long   id;
    private String requestId;
    private String accountType;
    private String reason;
    private String status;
    private String rejectionReason;
    private String reviewedBy;
    private String reviewedAt;
    private String createdAt;
    private String createdAccountNumber;
    // User info
    private String userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    // Existing accounts
    private java.util.List<Object> existingAccounts;
    // Latest application
    private Object latestApplication;
}

// ReviewAccountRequestDTO.java
