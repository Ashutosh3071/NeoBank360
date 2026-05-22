// src/main/java/com/neobank/dto/application/StatusOtpRequestDTO.java
package com.neobank.dto.application;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class StatusOtpRequestDTO {
    private String query;   // email OR applicationId
}