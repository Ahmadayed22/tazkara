package com.tazkara.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;


@Validated
@ConfigurationProperties(prefix = "tazkara.security")
public record SecurityProperties(@NotNull Jwt jwt, @NotBlank String internalApiKey) {

    public record Jwt(@NotBlank String secret, @NotBlank String issuer, Duration expiration) {

        public Jwt {
            if (expiration == null) {
                expiration = Duration.ofMinutes(30);
            }
        }
    }
}
