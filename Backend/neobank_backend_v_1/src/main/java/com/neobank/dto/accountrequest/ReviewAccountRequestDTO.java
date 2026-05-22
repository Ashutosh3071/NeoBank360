package com.neobank.dto.accountrequest;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ReviewAccountRequestDTO {
    private String  requestId;
    private String  action;  // APPROVE or REJECT
    private String  branchName;
    private String  branchCode;
    private String  ifscCode;
    private String  rejectionReason;
}