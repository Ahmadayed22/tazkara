package com.tazkara.booking.dto;

import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** One booking as seen by the event's organizer (US-15). */
public record OrganizerBookingResponse(
        String reference,
        String attendeeEmail,
        BookingStatus status,
        List<Tickets> tickets,
        BigDecimal totalAmount,
        Instant createdAt) {

    public record Tickets(String ticketTypeName, int quantity) {
    }

    public static OrganizerBookingResponse from(Booking booking) {
        return new OrganizerBookingResponse(
                booking.getReference().value(),
                booking.getAttendeeEmail(),
                booking.getStatus(),
                booking.getLines().stream().map(l -> new Tickets(l.ticketTypeName(), l.quantity())).toList(),
                booking.getTotalAmount(),
                booking.getCreatedAt());
    }
}
