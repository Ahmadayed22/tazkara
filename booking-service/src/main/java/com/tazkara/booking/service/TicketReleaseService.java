package com.tazkara.booking.service;

import com.tazkara.booking.domain.Booking;
import com.tazkara.common.messaging.MessagingTopology;
import com.tazkara.common.messaging.OutboxPublisher;
import com.tazkara.common.messaging.TicketsReleasedMessage;
import org.springframework.stereotype.Service;

import java.time.Clock;

/**
 * Tells the Event Service (asynchronously, through the outbox and RabbitMQ) that tickets are free again.
 * Asynchronous on purpose: the attendee does not have to wait for it and it must not fail just because the
 * Event Service is down; the Event Service handles a release idempotently.
 */
@Service
public class TicketReleaseService {

    private final OutboxPublisher outboxPublisher;
    private final Clock clock;

    public TicketReleaseService(OutboxPublisher outboxPublisher, Clock clock) {
        this.outboxPublisher = outboxPublisher;
        this.clock = clock;
    }

    /** Within the caller's transaction: committed together with the booking's status change. */
    public void release(Booking booking, String reason) {
        outboxPublisher.publish(MessagingTopology.TICKETS_RELEASED,
                new TicketsReleasedMessage(booking.getReference().value(), booking.getEventId(), reason, clock.instant()));
    }

    /**
     * Compensation when a reservation may exist on the Event Service side but the booking was not stored
     * (our transaction rolled back, or the reservation call timed out). Uses its own transaction.
     */
    public void compensate(String bookingReference, Long eventId) {
        outboxPublisher.publishInNewTransaction(MessagingTopology.TICKETS_RELEASED,
                new TicketsReleasedMessage(bookingReference, eventId, "BOOKING_NOT_STORED", clock.instant()));
    }
}
