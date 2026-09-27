package com.aivideo.platform.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientDiamondsException extends ApiException {
    public InsufficientDiamondsException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_DIAMONDS", message);
    }
}
