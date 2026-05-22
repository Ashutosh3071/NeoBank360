// src/main/java/com/neobank/dto/upi/CreateUpiRequestDTO.java

package com.neobank.dto.upi;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateUpiRequestDTO {
    private String accountNumber;  // which account to link
    private String vpaPrefix;      // user-chosen prefix — @neobank appended
    // NO phone — UPI ID is independent of phone
}