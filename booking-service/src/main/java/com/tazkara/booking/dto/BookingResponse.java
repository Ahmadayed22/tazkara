package com.tazkara.booking.dto;

import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingLine;
import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.domain.CancellationReason;
import com.tazkara.booking.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Attendee's view of a booking (US-10/11/13/14). Optional fields are omitted when empty. */
public record BookingResponse(
        String reference,
        Long eventId,
        String eventTitle,
        Instant eventStartsAt,
        BookingStatus status,
        List<Line> tickets,
        BigDecimal totalAmount,
        String currency,
        Instant holdExpiresAt,
        PaymentMethod paymentMethod,
        Instant paidAt,
        CancellationReason cancellationReason,
        Instant cancelledAt,
        BigDecimal refundAmount,
        Instant createdAt) {

    public record Line(Long ticketTypeId, String ticketTypeName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {

        static Line from(BookingLine line) {
            return new Line(line.ticketTypeId(), line.ticketTypeName(), line.unitPrice(), line.quantity(), line.lineTotal());
        }
    }

    public static BookingResponse from(Booking booking) {
        boolean pending = booking.getStatus() == BookingStatus.PENDING_PAYMENT;
        return new BookingResponse(
                booking.getReference().value(),
                booking.getEventId(),
                booking.getEventTitle(),
                booking.getEventStartsAt(),
                booking.getStatus(),
                booking.getLines().stream().map(Line::from).toList(),
                booking.getTotalAmount(),
                Booking.CURRENCY,
                pending ? booking.getHoldExpiresAt() : null,
                booking.getPaymentMethod(),
                booking.getPaidAt(),
                booking.getCancellationReason(),
                booking.getCancelledAt(),
                booking.getRefundAmount(),
                booking.getCreatedAt());
    }
}
