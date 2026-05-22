package com.neobank.dto.upi;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ChangeUpiPinRequestDTO {
    private String vpa;
    private String currentPin;
    private String newPin;
    private String confirmPin;
}

// UpiPayRequestDTO.java
