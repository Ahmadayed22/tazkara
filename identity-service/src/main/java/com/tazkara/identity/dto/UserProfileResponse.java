package com.tazkara.identity.dto;

import com.tazkara.common.security.Role;
import com.tazkara.identity.domain.User;

import java.time.Instant;


public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        String organizationName,
        Instant createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail().value(),
                user.getRole(), user.getOrganizationName(), user.getCreatedAt());
    }
}
