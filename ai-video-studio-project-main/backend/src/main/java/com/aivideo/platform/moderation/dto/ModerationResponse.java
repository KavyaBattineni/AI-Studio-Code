package com.aivideo.platform.moderation.dto;

public record ModerationResponse(boolean allowed, String message) {
}
