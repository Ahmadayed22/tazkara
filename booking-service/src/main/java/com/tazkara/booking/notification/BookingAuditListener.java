package com.tazkara.booking.notification;

import com.tazkara.booking.domain.BookingStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Second subscriber of the same domain event, added without touching any booking code:
 * this is the point of the Observer / Pub-Sub design of US-17.
 */
@Component
public class BookingAuditListener {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        log.info("booking={} event={} status={} reason={} refund={} at={}",
                event.bookingReference(), event.eventId(), event.newStatus(), event.cancellationReason(),
                event.refundAmount(), event.occurredAt());
    }
}
