package com.neobank.dto.user;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDTO3<T> {
    private LocalDateTime timestamp;
    private int status;
    private boolean success;
    private String message;
    private String errorCode;
    private T data;

    public static <T> ApiResponseDTO3<T> success(T data) {
        return ApiResponseDTO3.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponseDTO3<T> success(String message, T data) {
        return ApiResponseDTO3.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponseDTO3<T> error(int status, String message, String errorCode) {
        return ApiResponseDTO3.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}