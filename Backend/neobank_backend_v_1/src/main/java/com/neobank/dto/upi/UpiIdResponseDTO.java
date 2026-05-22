// src/main/java/com/neobank/dto/upi/UpiIdResponseDTO.java

package com.neobank.dto.upi;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpiIdResponseDTO {
    private Long    id;
    private String  vpa;
    private String  accountNumber;
    private String  accountType;
    private String  status;
    private Boolean isPrimary;
    private Boolean pinSet;
    private Long    dailyLimit;
    private Long    perTxnLimit;
    private String  createdAt;
    private String  lastUsedAt;
    private Boolean pinLocked;
    // NO linkedMobile
}