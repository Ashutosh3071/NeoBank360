package com.neobank.dto.budget;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetResponseDTO {
    private Long   id;
    private String category;
    private String budgetMonth;
    private String limitAmount;
    private String createdAt;
}

// BudgetSummaryDTO.java
