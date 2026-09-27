package com.aivideo.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when ModerationService blocks a prompt/image. The message on this exception
 * is always the generic, user-safe message - never the internal reason/keyword that
 * triggered the block (that detail is persisted to moderation_results for admins only).
 */
public class ModerationBlockedException extends ApiException {
    public ModerationBlockedException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "CONTENT_NOT_ALLOWED", message);
    }
}
