package com.tazkara.booking.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


/**
 * The Booking Service's own view of the Event Service internal API. Unknown fields are ignored so the
 * Event Service can add fields without breaking this client.
 */
public final class EventClientDtos {

    private EventClientDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventSnapshot(Long id, Long organizerId, String title, String status, Instant startsAt,
                                List<TicketTypeSnapshot> ticketTypes) {

        public boolean isCancelled() {
            return "CANCELLED".equals(status);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TicketTypeSnapshot(Long id, String name, BigDecimal price, int quantity, int availableTickets) {
    }

    public record ReserveTicketsRequest(String bookingReference, List<Line> lines) {

        public record Line(Long ticketTypeId, Integer quantity) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReservationResult(String bookingReference, Long eventId, String eventTitle, Instant eventStartsAt,
                                    List<ReservedLine> lines) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ReservedLine(Long ticketTypeId, String ticketTypeName, BigDecimal unitPrice, int quantity) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RemoteError(String code, String message) {
    }
}
