package com.tazkara.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.TicketType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicEventResponse(
        Long id,
        String title,
        String description,
        String venue,
        String city,
        Instant startsAt,
        boolean soldOut,
        List<TicketTypeAvailability> ticketTypes) {

    public record TicketTypeAvailability(Long id, String name, BigDecimal price, int availableTickets, boolean soldOut) {

        static TicketTypeAvailability from(TicketType type) {
            return new TicketTypeAvailability(type.getId(), type.getName(), type.getPrice(),
                    type.available(), type.isSoldOut());
        }
    }

    public static PublicEventResponse summary(Event event) {
        return of(event, null);
    }

    public static PublicEventResponse details(Event event) {
        return of(event, event.getDescription());
    }

    private static PublicEventResponse of(Event event, String description) {
        return new PublicEventResponse(event.getId(), event.getTitle(), description, event.getVenue(),
                event.getCity(), event.getStartsAt(), event.isSoldOut(),
                event.getTicketTypes().stream().map(TicketTypeAvailability::from).toList());
    }
}
