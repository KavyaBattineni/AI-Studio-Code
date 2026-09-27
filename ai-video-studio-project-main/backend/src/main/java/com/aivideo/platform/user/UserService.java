package com.aivideo.platform.user;

import com.aivideo.platform.common.exception.BadRequestException;
import com.aivideo.platform.common.exception.ResourceNotFoundException;
import com.aivideo.platform.user.dto.AdminUserResponse;
import com.aivideo.platform.user.dto.UpdateUserRequest;
import com.aivideo.platform.user.dto.UserResponse;
import com.aivideo.platform.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(getById(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserRequest request) {
        User user = getById(userId);
        user.setName(request.name());
        user.setMobile(request.mobile());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> adminSearch(String search, UserStatus status, Pageable pageable) {
        return userRepository.search(search, status, pageable)
                .map(u -> AdminUserResponse.from(u, walletRepository.findByUserId(u.getId())
                        .map(w -> w.getDiamondBalance()).orElse(0L)));
    }

    @Transactional
    public AdminUserResponse updateStatus(Long userId, String statusValue) {
        User user = getById(userId);
        UserStatus newStatus;
        try {
            newStatus = UserStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status value: " + statusValue);
        }
        user.setStatus(newStatus);
        User saved = userRepository.save(user);
        long balance = walletRepository.findByUserId(userId).map(w -> w.getDiamondBalance()).orElse(0L);
        return AdminUserResponse.from(saved, balance);
    }
}
