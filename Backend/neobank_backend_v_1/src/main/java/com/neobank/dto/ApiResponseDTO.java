package com.neobank.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO<T> {
    private LocalDateTime timestamp;
    private int status;
    private boolean success;
    private String message;
    private String errorCode;
    private T data;

    public static <T> ApiResponseDTO<T> success(T data) {
        return ApiResponseDTO.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponseDTO<T> success(String message, T data) {
        return ApiResponseDTO.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponseDTO<T> error(int status, String message, String errorCode) {
        return ApiResponseDTO.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}