// BillRequestDTO.java
package com.neobank.dto.bill;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BillRequestDTO {
    private String billerName;
    private String amount;
    private String dueDate;    // YYYY-MM-DD
    private String description;
}

// BillResponseDTO.java
