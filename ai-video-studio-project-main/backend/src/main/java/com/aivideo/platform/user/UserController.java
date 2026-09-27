package com.aivideo.platform.user;

import com.aivideo.platform.common.ApiResponse;
import com.aivideo.platform.security.SecurityUser;
import com.aivideo.platform.user.dto.UpdateUserRequest;
import com.aivideo.platform.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.success(userService.getProfile(user.getId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMe(@AuthenticationPrincipal SecurityUser user,
                                               @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("Profile updated", userService.updateProfile(user.getId(), request));
    }
}
