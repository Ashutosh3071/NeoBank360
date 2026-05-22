package com.neobank.dto.upi;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class SetUpiPinRequestDTO {
    private String vpa;
    private String otp;          // verified first
    private String newPin;       // 4 or 6 digit
    private String confirmPin;
}

// ChangeUpiPinRequestDTO.java
