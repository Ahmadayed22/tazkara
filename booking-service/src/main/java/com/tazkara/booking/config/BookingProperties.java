package com.tazkara.booking.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Business and integration settings of the Booking Service, bound from {tazkara.booking.*}. */
@Validated
@ConfigurationProperties(prefix = "tazkara.booking")
public record BookingProperties(
        @NotNull Duration holdDuration,
        @Min(1) int maxTicketsPerEvent,
        @Min(1) int expiryBatchSize,
        @NotNull @Valid EventService eventService) {

    public record EventService(@NotBlank String baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {
    }
}
