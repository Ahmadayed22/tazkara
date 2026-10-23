package com.tazkara.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Ahmad A.Obaid") String fullName,
        @Size(max = 150) @Schema(description = "Organizers only; must not be blank for organizers") String organizationName) {
}
