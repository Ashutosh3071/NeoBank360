package com.neobank.dto.upi;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpiTransactionResponseDTO {
    private Long   id;
    private String upiReference;
    private String senderVpa;
    private String receiverVpa;
    private String amount;
    private String description;
    private String status;
    private String createdAt;
    private String type;    // SENT or RECEIVED
}