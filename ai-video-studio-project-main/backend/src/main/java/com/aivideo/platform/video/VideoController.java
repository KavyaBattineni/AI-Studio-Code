package com.aivideo.platform.video;

import com.aivideo.platform.common.ApiResponse;
import com.aivideo.platform.security.SecurityUser;
import com.aivideo.platform.video.dto.VideoGenerationRequest;
import com.aivideo.platform.video.dto.VideoJobResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Tag(name = "Videos")
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<VideoJobResponse> generate(@AuthenticationPrincipal SecurityUser user,
                                                    @Valid @RequestBody VideoGenerationRequest request) {
        return ApiResponse.success("Video generation started", videoService.generate(user.getId(), request));
    }

    @GetMapping
    public ApiResponse<Page<VideoJobResponse>> list(@AuthenticationPrincipal SecurityUser user,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(videoService.listJobs(user.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoJobResponse> get(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        return ApiResponse.success(videoService.getJob(user.getId(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        videoService.deleteJob(user.getId(), id);
        return ApiResponse.success("Video deleted");
    }
}
