// src/main/java/com/neobank/dto/DevMailDTO.java

package com.neobank.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DevMailDTO {
    private Long id;
    private String recipient;
    private String subject;
    private String body;
    private String otp;           // null if no OTP in body
    private boolean seen;
    private LocalDateTime createdAt;
}