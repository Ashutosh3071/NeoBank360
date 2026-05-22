// src/main/java/com/neobank/dto/transaction/AdminTransactionRequestDTO.java

package com.neobank.dto.transaction;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminTransactionRequestDTO {
    private String accountNumber;
    private String amount;
    private String type;          // DEPOSIT or WITHDRAWAL
    private String description;
    private String remarks;
}