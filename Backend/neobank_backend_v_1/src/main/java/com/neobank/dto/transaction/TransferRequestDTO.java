// src/main/java/com/neobank/dto/transaction/TransferRequestDTO.java

package com.neobank.dto.transaction;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferRequestDTO {
    private String fromAccountNumber;
    private String toAccountNumber;    // internal account number
    private String amount;
    private String description;
    private String mode;               // TRANSFER, UPI, NEFT
    // UPI fields
    private String upiId;
    // NEFT fields
    private String beneficiaryName;
    private String beneficiaryIfsc;
    private String beneficiaryBankName;
}