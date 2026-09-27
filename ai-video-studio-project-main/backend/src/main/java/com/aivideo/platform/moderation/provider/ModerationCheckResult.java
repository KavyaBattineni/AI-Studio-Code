package com.aivideo.platform.moderation.provider;

/** internalReason is for logging/admin review only - never shown to the end user. */
public record ModerationCheckResult(boolean allowed, String internalReason) {

    public static ModerationCheckResult allowed() {
        return new ModerationCheckResult(true, null);
    }

    public static ModerationCheckResult blocked(String internalReason) {
        return new ModerationCheckResult(false, internalReason);
    }
}
