package com.tazkara.identity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tazkara.common.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100)
        @Schema(example = "ahmad ayed")
        String fullName,

        @NotBlank @Email @Size(max = 254)
        @Schema(example = "ahmad@gmail.com")
        String email,

        @NotBlank
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain at least one letter and one digit")
        @Schema(example = "Ahmad123")
        String password,

        @NotNull
        @Schema(example = "ATTENDEE", allowableValues = {"ATTENDEE", "ORGANIZER"})
        Role role,

        @Size(max = 150)
        @Schema(description = "Required when role is ORGANIZER, ignored for attendees", example = "Amman Live Events")
        String organizationName) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "organizationName is required for organizers")
    public boolean isOrganizationNameProvided() {
        return role != Role.ORGANIZER || (organizationName != null && !organizationName.isBlank());
    }
}
