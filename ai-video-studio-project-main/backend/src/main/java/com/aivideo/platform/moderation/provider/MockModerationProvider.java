package com.aivideo.platform.moderation.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Development/demo moderation provider. It does a simple case-insensitive keyword match
 * against a short illustrative blocklist covering the categories this platform must reject
 * (pornographic/explicit sexual content, sexual exploitation, and - zero tolerance, no
 * exceptions - any sexual content involving minors).
 *
 * ⚠️ THIS IS NOT A PRODUCTION-GRADE FILTER. Keyword matching is trivially bypassed with
 * misspellings, synonyms, other languages, or indirect phrasing. Before going live, replace
 * this with a real moderation API or a trained classifier (e.g. a hosted text/image
 * moderation endpoint) behind the same ModerationProvider interface, selected via
 * app.mode=live. Keep the blocklist itself out of source control in that implementation too -
 * treat it as sensitive configuration, not a code constant.
 */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "mock", matchIfMissing = true)
public class MockModerationProvider implements ModerationProvider {

    // Deliberately short and illustrative, not exhaustive - see class comment above.
    private static final List<String> BLOCKED_TERMS = List.of(
            "porn", "pornographic", "explicit sexual", "sexual act", "nude photo", "nsfw",
            "child sexual", "csam", "sexual exploitation", "xxx video"
    );

    @Override
    public ModerationCheckResult checkText(String text) {
        if (text == null || text.isBlank()) {
            return ModerationCheckResult.allowed();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String term : BLOCKED_TERMS) {
            if (normalized.contains(term)) {
                return ModerationCheckResult.blocked("matched-blocklist-term");
            }
        }
        return ModerationCheckResult.allowed();
    }

    @Override
    public ModerationCheckResult checkImage(byte[] imageBytes, String contentType) {
        // No real image classifier in mock mode. Fail safe would normally mean "block", but
        // since image-to-video isn't wired into the UI yet (see VideoService), this simply
        // allows so local development isn't blocked on a feature that isn't exposed anywhere.
        // A real ModerationProvider implementation MUST actually classify the image bytes.
        return ModerationCheckResult.allowed();
    }
}
