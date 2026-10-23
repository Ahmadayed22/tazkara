package com.tazkara.identity.controller;
import com.tazkara.common.security.AuthenticatedUser;
import com.tazkara.identity.service.ProfileService;
import com.tazkara.identity.dto.UpdateProfileRequest;
import com.tazkara.identity.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Profile", description = "US-03 view and update my profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(summary = "Get my profile")
    public UserProfileResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return profileService.getProfile(user.id());
    }

    @PutMapping
    @Operation(summary = "Update my name and organization name (email and role cannot change)")
    public UserProfileResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(user.id(), request);
    }
}
