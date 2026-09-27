package com.aivideo.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for every business exception in the application. GlobalExceptionHandler
 * turns these into the standard ErrorResponse envelope using the status/errorCode here,
 * so individual controllers never need to build error responses by hand.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
