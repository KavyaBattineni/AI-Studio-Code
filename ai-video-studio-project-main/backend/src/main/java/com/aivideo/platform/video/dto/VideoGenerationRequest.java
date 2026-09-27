package com.aivideo.platform.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VideoGenerationRequest(
        @NotBlank(message = "Prompt is required")
        @Size(max = 2000, message = "Prompt must be 2000 characters or fewer")
        String prompt,

        @NotNull(message = "Duration is required")
        Integer duration
) {
}
