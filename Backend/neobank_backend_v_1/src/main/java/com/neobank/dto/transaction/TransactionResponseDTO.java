// src/main/java/com/neobank/dto/transaction/TransactionResponseDTO.java

package com.neobank.dto.transaction;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionResponseDTO {
    private Long   id;
    private String referenceNumber;
    private String transactionType;
    private String transactionMode;
    private String amount;
    private String charges;
    private String netAmount;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String beneficiaryName;
    private String upiId;
    private String description;
    private String status;
    private String balanceAfter;   // from customer's perspective
    private String createdAt;
    private String processedAt;
}