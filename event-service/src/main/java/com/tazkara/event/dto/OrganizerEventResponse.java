package com.tazkara.event.dto;

import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.EventStatus;
import com.tazkara.event.domain.TicketType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Owner's view of an event in any status (US-04/05/06/08/09). */
public record OrganizerEventResponse(
        Long id,
        String title,
        String description,
        String venue,
        String city,
        Instant startsAt,
        EventStatus status,
        Instant createdAt,
        Instant publishedAt,
        Instant cancelledAt,
        List<TicketTypeInventory> ticketTypes) {

    public record TicketTypeInventory(Long id, String name, BigDecimal price, int quantity, int availableTickets) {

        static TicketTypeInventory from(TicketType type) {
            return new TicketTypeInventory(type.getId(), type.getName(), type.getPrice(), type.getQuantity(),
                    type.available());
        }
    }

    public static OrganizerEventResponse from(Event event) {
        return new OrganizerEventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getVenue(),
                event.getCity(), event.getStartsAt(), event.getStatus(), event.getCreatedAt(),
                event.getPublishedAt(), event.getCancelledAt(),
                event.getTicketTypes().stream().map(TicketTypeInventory::from).toList());
    }
}
