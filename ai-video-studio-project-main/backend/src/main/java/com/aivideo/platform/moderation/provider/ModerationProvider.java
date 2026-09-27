package com.aivideo.platform.moderation.provider;

/**
 * Abstraction over whatever content-safety check backs prompt/image moderation.
 * Implement this against a real moderation API or ML classifier for production
 * (see MockModerationProvider's class comment for integration notes) and select it
 * with app.mode=live. Never call an AI video provider before this check passes.
 */
public interface ModerationProvider {

    ModerationCheckResult checkText(String text);

    ModerationCheckResult checkImage(byte[] imageBytes, String contentType);
}
