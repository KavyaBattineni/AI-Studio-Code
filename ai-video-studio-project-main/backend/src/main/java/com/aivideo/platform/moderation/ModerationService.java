package com.aivideo.platform.moderation;

import com.aivideo.platform.common.exception.ModerationBlockedException;
import com.aivideo.platform.moderation.entity.ModerationDecision;
import com.aivideo.platform.moderation.entity.ModerationInputType;
import com.aivideo.platform.moderation.entity.ModerationResult;
import com.aivideo.platform.moderation.provider.ModerationCheckResult;
import com.aivideo.platform.moderation.provider.ModerationProvider;
import com.aivideo.platform.user.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);
    private static final String GENERIC_BLOCKED_MESSAGE = "This request isn't allowed. Please try a different prompt.";
    private static final String GENERIC_UNAVAILABLE_MESSAGE = "Content verification is temporarily unavailable. Please try again.";
    private static final int SNIPPET_MAX_LENGTH = 500;

    private final ModerationProvider moderationProvider;
    private final ModerationRepository moderationRepository;

    /**
     * Checks a prompt BEFORE any diamonds are touched or any video job is created. Throws
     * ModerationBlockedException (caught by GlobalExceptionHandler, surfaced as 422 with the
     * generic message only) if the content is not allowed. If the provider itself fails/throws,
     * this defaults to BLOCKING the request - moderation failure must never silently allow
     * generation through.
     */
    @Transactional
    public void assertPromptAllowed(User user, String prompt, String referenceId) {
        ModerationCheckResult result;
        try {
            result = moderationProvider.checkText(prompt);
        } catch (Exception ex) {
            log.error("Moderation provider failed; defaulting to blocked", ex);
            persist(user, ModerationInputType.PROMPT, prompt, ModerationDecision.BLOCKED, "provider-unavailable", referenceId);
            throw new ModerationBlockedException(GENERIC_UNAVAILABLE_MESSAGE);
        }

        persist(user, ModerationInputType.PROMPT, prompt,
                result.allowed() ? ModerationDecision.ALLOWED : ModerationDecision.BLOCKED,
                result.internalReason(), referenceId);

        if (!result.allowed()) {
            throw new ModerationBlockedException(GENERIC_BLOCKED_MESSAGE);
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.aivideo.platform.moderation.dto.AdminModerationResponse> adminSearch(
            ModerationDecision decision, org.springframework.data.domain.Pageable pageable) {
        return moderationRepository.adminSearch(decision, pageable)
                .map(com.aivideo.platform.moderation.dto.AdminModerationResponse::from);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public long countBlocked() {
        return moderationRepository.countByDecision(ModerationDecision.BLOCKED);
    }

    private void persist(User user, ModerationInputType inputType, String rawInput,
                          ModerationDecision decision, String reason, String referenceId) {
        String snippet = rawInput == null ? null
                : rawInput.substring(0, Math.min(rawInput.length(), SNIPPET_MAX_LENGTH));
        ModerationResult result = ModerationResult.builder()
                .user(user)
                .inputType(inputType)
                .inputSnippet(snippet)
                .decision(decision)
                .reason(reason)
                .referenceId(referenceId)
                .build();
        moderationRepository.save(result);
    }
}
