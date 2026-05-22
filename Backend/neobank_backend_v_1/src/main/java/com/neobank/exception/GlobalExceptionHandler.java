package com.neobank.exception;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.service.ApplicationService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NeoBankException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleNeoBankException(NeoBankException ex) {
        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>error(
                ex.getHttpStatus().value(),
                ex.getMessage(),
                ex.getErrorCode()
        );
       log.error(ex.toString());
        return new ResponseEntity<>(response, ex.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(Exception ex) {
        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                "INTERNAL_ERROR"
        );
        log.error(ex.toString());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
