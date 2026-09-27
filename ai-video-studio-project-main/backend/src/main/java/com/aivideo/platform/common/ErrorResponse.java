package com.aivideo.platform.common;

import java.time.Instant;

/**
 * Standard error envelope returned by every failed REST call.
 *
 * {
 *   "success": false,
 *   "message": "Insufficient diamonds",
 *   "errorCode": "INSUFFICIENT_DIAMONDS",
 *   "timestamp": "2026-09-18T10:15:30Z"
 * }
 */
public record ErrorResponse(boolean success, String message, String errorCode, Instant timestamp) {

    public static ErrorResponse of(String message, String errorCode) {
        return new ErrorResponse(false, message, errorCode, Instant.now());
    }
}
