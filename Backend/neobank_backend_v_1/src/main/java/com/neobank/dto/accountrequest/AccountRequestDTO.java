// AccountRequestDTO.java
package com.neobank.dto.accountrequest;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountRequestDTO {
    private String accountType;
    private String reason;
}

// AccountRequestResponseDTO.java
