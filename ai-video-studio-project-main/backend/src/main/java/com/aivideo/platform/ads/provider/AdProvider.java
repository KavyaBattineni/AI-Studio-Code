package com.aivideo.platform.ads.provider;

/**
 * Abstraction over the rewarded-ad network's server-side reward verification. Implement this
 * against your chosen ad network's actual server-to-server verification/reporting API for
 * production (e.g. validating their reward callback signature) and select it via
 * app.mode=live plus AD_PROVIDER_KEY. RewardService NEVER credits diamonds based on a client
 * claiming "I watched the ad" alone - this verification step is mandatory.
 */
public interface AdProvider {

    AdVerificationResult verifyReward(String verificationId);
}
