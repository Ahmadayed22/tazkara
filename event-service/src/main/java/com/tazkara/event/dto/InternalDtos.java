package com.tazkara.event.dto;


import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.EventStatus;
import com.tazkara.event.domain.TicketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


/**
 * Contracts of the internal (service-to-service) API used by the Booking Service.
 */
public final class InternalDtos {

    private InternalDtos() {
    }

    public record EventSnapshot(Long id, Long organizerId, String title, EventStatus status, Instant startsAt,
                                List<TicketTypeSnapshot> ticketTypes) {

        public static EventSnapshot from(Event event) {
            return new EventSnapshot(event.getId(), event.getOrganizerId(), event.getTitle(), event.getStatus(),
                    event.getStartsAt(), event.getTicketTypes().stream().map(TicketTypeSnapshot::from).toList());
        }
    }

    public record TicketTypeSnapshot(Long id, String name, BigDecimal price, int quantity, int availableTickets) {

        static TicketTypeSnapshot from(TicketType type) {
            return new TicketTypeSnapshot(type.getId(), type.getName(), type.getPrice(), type.getQuantity(),
                    type.available());
        }
    }

    public record ReserveTicketsRequest(@NotBlank String bookingReference,
                                        @NotEmpty List<@Valid @NotNull Line> lines) {

        public record Line(@NotNull Long ticketTypeId, @NotNull @Min(1) @Max(10_000) Integer quantity) {
        }
    }

    /** Prices are read from the event at reservation time; the Booking Service stores them as they are. */
    public record ReservationResponse(String bookingReference, Long eventId, String eventTitle, Instant eventStartsAt,
                                      List<ReservedLine> lines) {

        public record ReservedLine(Long ticketTypeId, String ticketTypeName, BigDecimal unitPrice, int quantity) {
        }
    }
}
