// src/main/java/com/neobank/dto/account/AccountStatusUpdateDTO.java

package com.neobank.dto.account;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AccountStatusUpdateDTO {
    private String accountNumber;
    private String status;         // ACTIVE, FROZEN, SUSPENDED, CLOSED
    private String reason;
}