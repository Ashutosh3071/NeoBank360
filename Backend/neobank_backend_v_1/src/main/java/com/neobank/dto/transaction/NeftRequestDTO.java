// NeftRequestDTO.java
package com.neobank.dto.transaction;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NeftRequestDTO {
    private String fromAccountNumber;
    private String beneficiaryAccountNumber;
    private String beneficiaryName;
    private String beneficiaryIfsc;
    private String beneficiaryBankName;
    private String amount;
    private String description;
    private String mode;  // NEFT or RTGS
}