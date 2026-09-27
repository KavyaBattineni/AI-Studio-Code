package com.aivideo.platform.storage;

/**
 * Object-storage abstraction. Generated video/thumbnail bytes are never stored in MySQL -
 * only the resulting URL and storage key are persisted on VideoJob. Swap MockStorageService
 * for a real S3StorageService (AWS SDK v2: software.amazon.awssdk:s3) behind app.mode=live;
 * it should implement this same interface using STORAGE_BUCKET / STORAGE_ACCESS_KEY /
 * STORAGE_SECRET_KEY from application.properties, and ideally return CDN-fronted URLs.
 */
public interface StorageService {

    String uploadVideo(String key, byte[] content, String contentType);

    String uploadThumbnail(String key, byte[] content, String contentType);

    void deleteVideo(String key);

    String getVideoUrl(String key);
}
