// src/main/java/com/neobank/dto/application/SendOtpRequestDTO.java

package com.neobank.dto.application;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequestDTO {
    private String email;
}