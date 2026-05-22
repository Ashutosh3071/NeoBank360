package com.neobank.dto.budget;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetSummaryDTO {
    private String category;
    private String limitAmount;
    private String spentAmount;
    private String remainingAmount;
    private Double utilizationPercent;
    private String status;   // SAFE / WARNING / EXCEEDED
}

// BudgetSummaryResponseDTO.java
