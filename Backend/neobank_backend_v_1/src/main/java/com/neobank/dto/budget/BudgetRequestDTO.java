// BudgetRequestDTO.java
package com.neobank.dto.budget;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BudgetRequestDTO {
    private String category;     // GROCERIES, UTILITIES etc.
    private String budgetMonth;  // YYYY-MM
    private String limitAmount;
}

// BudgetResponseDTO.java
