package com.neobank.dto.budget;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetSummaryResponseDTO {
    private String              month;
    private String              totalLimit;
    private String              totalSpent;
    private Double              overallUtilization;
    private java.util.List<BudgetSummaryDTO> categories;
}