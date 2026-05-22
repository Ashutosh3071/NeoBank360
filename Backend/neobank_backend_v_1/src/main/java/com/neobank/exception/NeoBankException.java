package com.neobank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NeoBankException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public NeoBankException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public static NeoBankException userNotFound() {
        return new NeoBankException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
    }

}