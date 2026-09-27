package com.aivideo.platform.common;

/**
 * Standard success envelope returned by every REST endpoint in this API.
 *
 * {
 *   "success": true,
 *   "message": "Operation successful",
 *   "data": { ... }
 * }
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
