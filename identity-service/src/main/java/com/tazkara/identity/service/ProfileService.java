package com.tazkara.identity.service;

import com.tazkara.common.error.DomainException;
import com.tazkara.identity.domain.User;
import com.tazkara.identity.exception.IdentityErrorCode;
import com.tazkara.identity.repository.UserRepository;
import com.tazkara.identity.dto.UpdateProfileRequest;
import com.tazkara.identity.dto.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;


@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return UserProfileResponse.from(load(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = load(userId);
        user.updateProfile(request.fullName(), request.organizationName(), clock.instant());
        return UserProfileResponse.from(user);
    }

    private User load(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(IdentityErrorCode.USER_NOT_FOUND, "User not found"));
    }
}
