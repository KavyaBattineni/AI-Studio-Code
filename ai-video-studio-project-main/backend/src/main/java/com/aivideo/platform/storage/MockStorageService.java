package com.aivideo.platform.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development storage provider. Doesn't write to any real bucket - it just tracks which
 * keys "exist" in memory and returns deterministic, clearly-fake URLs under the configured
 * mock base URL. Good enough to exercise the full generate -> store -> list -> play ->
 * delete flow in the UI without any cloud credentials.
 */
@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "mock", matchIfMissing = true)
public class MockStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MockStorageService.class);
    private final Set<String> storedKeys = ConcurrentHashMap.newKeySet();

    @Value("${app.providers.storage.public-base-url:https://mock-storage.aivideostudio.local}")
    private String baseUrl;

    @Override
    public String uploadVideo(String key, byte[] content, String contentType) {
        storedKeys.add(key);
        log.info("[MOCK STORAGE] 'Uploaded' video object key={} bytes={}", key, content == null ? 0 : content.length);
        return baseUrl + "/" + key;
    }

    @Override
    public String uploadThumbnail(String key, byte[] content, String contentType) {
        storedKeys.add(key);
        log.info("[MOCK STORAGE] 'Uploaded' thumbnail object key={}", key);
        return baseUrl + "/" + key;
    }

    @Override
    public void deleteVideo(String key) {
        storedKeys.remove(key);
        log.info("[MOCK STORAGE] 'Deleted' object key={}", key);
    }

    @Override
    public String getVideoUrl(String key) {
        return baseUrl + "/" + key;
    }
}
