package com.tazkara.event.controller;

import com.tazkara.common.web.PageResponse;
import com.tazkara.common.web.Paging;
import com.tazkara.event.service.PublicEventService;
import com.tazkara.event.dto.PublicEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Validated
@RequestMapping("/api/events")
@Tag(name = "Public events", description = "US-07 browse and view events (no login needed)")
@SecurityRequirements
public class PublicEventController {

    private final PublicEventService publicEventService;

    public PublicEventController(PublicEventService publicEventService) {
        this.publicEventService = publicEventService;
    }

    @GetMapping
    @Operation(summary = "Browse upcoming published events, soonest first. Filters can be combined.")
    public PageResponse<PublicEventResponse> browse(
            @RequestParam(required = false) @Size(max = 100) String city,
            @Parameter(description = "First day (UTC), inclusive", example = "2026-11-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day (UTC), inclusive", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Case-insensitive text searched in the title")
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE_TEXT) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return publicEventService.browse(city, from, to, keyword, page, size);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Event details including the full description")
    public PublicEventResponse details(@PathVariable Long eventId) {
        return publicEventService.details(eventId);
    }
}
