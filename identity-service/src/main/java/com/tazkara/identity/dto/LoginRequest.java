package com.tazkara.identity.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Schema(example = "ahmad@tazkara.com") String email,
        @NotBlank @Schema(example = "Ahmad123") String password) {
}
