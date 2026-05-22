// src/main/java/com/neobank/dto/application/StatusVerifyRequestDTO.java
package com.neobank.dto.application;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class StatusVerifyRequestDTO {
    private String query;   // email OR applicationId
    private String otp;
}