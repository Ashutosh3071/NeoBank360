package com.neobank.dto.upi;

import lombok.*;


@Data @NoArgsConstructor @AllArgsConstructor
public class UpiPayRequestDTO {
    private String senderVpa;
    private String receiverVpa;
    private String amount;
    private String upiPin;
    private String description;
    private String requestType;  // PAY or REQUEST
}

// UpiIdResponseDTO.java
