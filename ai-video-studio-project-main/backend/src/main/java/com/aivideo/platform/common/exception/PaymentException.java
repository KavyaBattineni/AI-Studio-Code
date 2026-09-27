package com.aivideo.platform.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends ApiException {
    public PaymentException(String message) {
        super(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", message);
    }
}
