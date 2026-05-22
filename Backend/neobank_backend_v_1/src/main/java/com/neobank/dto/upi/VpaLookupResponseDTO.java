// src/main/java/com/neobank/dto/upi/VpaLookupResponseDTO.java

package com.neobank.dto.upi;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VpaLookupResponseDTO {
    private String  query;               // what user typed (VPA or phone)
    private String  resolvedVpa;         // actual VPA always stored
    private String  accountHolderName;
    private String  maskedAccountNumber;
    private Boolean valid;
    private String  message;             // error message if not valid
}