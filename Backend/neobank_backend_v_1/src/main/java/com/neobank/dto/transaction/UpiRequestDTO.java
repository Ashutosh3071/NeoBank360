// UpiRequestDTO.java
package com.neobank.dto.transaction;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpiRequestDTO {
    private String fromAccountNumber;
    private String upiId;           // e.g. john@upi or 9876543210@upi
    private String amount;
    private String description;
}