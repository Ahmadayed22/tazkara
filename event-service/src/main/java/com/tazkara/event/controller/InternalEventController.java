package com.tazkara.event.controller;


import com.tazkara.event.service.ReservationService;
import com.tazkara.event.dto.InternalDtos.EventSnapshot;
import com.tazkara.event.dto.InternalDtos.ReservationResponse;
import com.tazkara.event.dto.InternalDtos.ReserveTicketsRequest;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service API for the Booking Service, protected by the shared internal API key
 * ( InternalApiKeyFilter). Hidden from the public Swagger UI.
 */
@Hidden
@RestController
@RequestMapping("/internal/events")
public class InternalEventController {

    private final ReservationService reservationService;

    public InternalEventController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{eventId}")
    public EventSnapshot event(@PathVariable Long eventId) {
        return reservationService.snapshot(eventId);
    }

    @PostMapping("/{eventId}/reservations")
    public ReservationResponse reserve(@PathVariable Long eventId, @Valid @RequestBody ReserveTicketsRequest request) {
        return reservationService.reserve(eventId, request);
    }
}
