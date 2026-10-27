package com.tazkara.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** US-10 request. Prices are never sent by the client. */
public record PlaceBookingRequest(
        @NotNull @Schema(example = "1") Long eventId,
        @NotEmpty @Size(max = 5) List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull @Schema(example = "1") Long ticketTypeId,
            @NotNull @Min(1) @Max(6) @Schema(example = "2") Integer quantity) {
    }

    /** Lines for the same ticket type are merged (e.g. 2 + 1 Standard = 3 Standard). */
    public Map<Long, Integer> quantitiesByTicketType() {
        return items.stream().collect(Collectors.toMap(Item::ticketTypeId, Item::quantity, Integer::sum,
                LinkedHashMap::new));
    }
}
