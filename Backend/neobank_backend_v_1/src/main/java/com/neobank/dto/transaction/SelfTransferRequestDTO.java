// SelfTransferRequestDTO.java
package com.neobank.dto.transaction;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SelfTransferRequestDTO {
    private String fromAccountNumber;
    private String toAccountNumber;
    private String amount;
    private String description;
}