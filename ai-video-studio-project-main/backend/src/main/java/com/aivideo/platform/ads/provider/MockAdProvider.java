package com.aivideo.platform.ads.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development ad-network provider. Accepts any non-blank verification id as proof the
 * (simulated) rewarded ad was watched in full - good enough to exercise the whole
 * watch-ad -> verify -> credit-diamonds flow with no ad network account.
 *
 * Swap for a real adapter (implementing AdProvider against your ad network's actual
 * server-side verification API) selected via app.mode=live plus AD_PROVIDER_KEY.
 */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "mock", matchIfMissing = true)
public class MockAdProvider implements AdProvider {

    private static final Logger log = LoggerFactory.getLogger(MockAdProvider.class);

    @Override
    public AdVerificationResult verifyReward(String verificationId) {
        if (verificationId == null || verificationId.isBlank()) {
            return new AdVerificationResult(false, "Missing verification id");
        }
        log.info("[MOCK AD PROVIDER] Verified rewarded-ad completion id={}", verificationId);
        return new AdVerificationResult(true, "Ad watch verified");
    }
}
