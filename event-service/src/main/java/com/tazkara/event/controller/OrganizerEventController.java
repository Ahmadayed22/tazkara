package com.tazkara.event.controller;

import com.tazkara.common.security.AuthenticatedUser;
import com.tazkara.common.web.PageResponse;
import com.tazkara.common.web.Paging;
import com.tazkara.event.domain.EventStatus;
import com.tazkara.event.service.OrganizerEventService;
import com.tazkara.event.dto.EventRequest;
import com.tazkara.event.dto.OrganizerEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@PreAuthorize("hasRole('ORGANIZER')")
@Tag(name = "Organizer events", description = "US-04/05/06/08/09 manage my events (ORGANIZER only)")
public class OrganizerEventController {

    private final OrganizerEventService organizerEventService;

    public OrganizerEventController(OrganizerEventService organizerEventService) {
        this.organizerEventService = organizerEventService;
    }

    @PostMapping("/api/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a DRAFT event with 1 to 5 ticket types")
    public OrganizerEventResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody EventRequest request) {
        return organizerEventService.create(user, request);
    }

    @PutMapping("/api/events/{eventId}")
    @Operation(summary = "Edit my event while it is still a DRAFT (replaces details and ticket types)")
    public OrganizerEventResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable Long eventId,
                                         @Valid @RequestBody EventRequest request) {
        return organizerEventService.updateDraft(user, eventId, request);
    }

    @PostMapping("/api/events/{eventId}/publish")
    @Operation(summary = "Publish my DRAFT event (start must still be 24h+ away)")
    public OrganizerEventResponse publish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long eventId) {
        return organizerEventService.publish(user, eventId);
    }

    @PostMapping("/api/events/{eventId}/cancel")
    @Operation(summary = "Cancel my PUBLISHED event; its bookings are cancelled and refunded asynchronously")
    public OrganizerEventResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long eventId) {
        return organizerEventService.cancel(user, eventId);
    }

    @GetMapping("/api/organizer/events")
    @Operation(summary = "List my events in any status, newest first, optionally filtered by status")
    public PageResponse<OrganizerEventResponse> listMine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = Paging.DEFAULT_SIZE_TEXT) @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return organizerEventService.listOwn(user, status, page, size);
    }

    @GetMapping("/api/organizer/events/{eventId}")
    @Operation(summary = "One of my events in any status")
    public OrganizerEventResponse getMine(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long eventId) {
        return organizerEventService.getOwn(user, eventId);
    }
}
