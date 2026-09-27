package com.aivideo.platform.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 is used here purely to fingerprint short-lived, high-entropy random tokens
 * (OTP codes, refresh token values) so the plaintext is never at rest in the database.
 * This is not password hashing - user passwords always go through BCrypt (see SecurityConfig).
 */
public final class HashUtil {

    private HashUtil() {
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
