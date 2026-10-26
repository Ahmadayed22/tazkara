package com.tazkara.booking.domain;


import java.math.BigDecimal;
import java.time.Instant;


public record BookingStatusChangedEvent(
        String bookingReference,
        String attendeeEmail,
        Long eventId,
        String eventTitle,
        BookingStatus newStatus,
        CancellationReason cancellationReason,
        BigDecimal totalAmount,
        BigDecimal refundAmount,
        Instant occurredAt) {
}
