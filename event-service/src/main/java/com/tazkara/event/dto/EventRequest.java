package com.tazkara.event.dto;

import com.tazkara.event.domain.EventDetails;
import com.tazkara.event.domain.TicketTypeSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Body of create (US-04) and edit-draft (US-05). Business rules (24h lead time, unique names) live in the domain. */
public record EventRequest(
        @NotBlank @Size(max = 150) @Schema(example = "Jazz Night at the Citadel") String title,
        @NotBlank @Size(max = 5000) @Schema(example = "An evening of live jazz under the stars.") String description,
        @NotBlank @Size(max = 150) @Schema(example = "Amman Citadel") String venue,
        @NotBlank @Size(max = 100) @Schema(example = "Amman") String city,
        @NotNull @Schema(description = "ISO-8601 instant in UTC, at least 24 hours from now", example = "2026-12-20T18:00:00Z")
        Instant startsAt,
        @NotNull @Size(min = 1, max = 5, message = "must contain between 1 and 5 ticket types")
        List<@Valid @NotNull TicketTypeRequest> ticketTypes) {

    public record TicketTypeRequest(
            @NotBlank @Size(max = 50) @Schema(example = "Standard") String name,
            @NotNull @DecimalMin(value = "0.00", message = "must be zero or more")
            @Digits(integer = 8, fraction = 2) @Schema(example = "25.00") BigDecimal price,
            @NotNull @Min(1) @Max(10_000) @Schema(example = "200") Integer quantity) {
    }

    public EventDetails toDetails() {
        return new EventDetails(title, description, venue, city, startsAt);
    }

    public List<TicketTypeSpec> toSpecs() {
        return ticketTypes.stream()
                .map(t -> new TicketTypeSpec(t.name(), t.price(), t.quantity()))
                .toList();
    }
}
