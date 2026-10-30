package com.tazkara.booking.notification;

import com.tazkara.booking.config.AsyncConfig;
import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingStatusChangedEvent;
import com.tazkara.booking.domain.CancellationReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * US-17 subscriber: "sends" an email for each booking status change (simulated with a log line).
 * Runs only after the transaction committed (never notifies about a change that was rolled back) and on the
 * notification thread pool, so a slow mail server would never slow the attendee's request.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        Notification notification = compose(event);
        log.info("[EMAIL] to={} | subject=\"{}\" | message=\"{}\"",
                event.attendeeEmail(), notification.subject(), notification.message());
    }

    private static Notification compose(BookingStatusChangedEvent event) {
        String ref = event.bookingReference();
        String title = event.eventTitle();
        return switch (event.newStatus()) {
            case CONFIRMED -> new Notification(
                    "Booking " + ref + " confirmed",
                    "Your tickets for '" + title + "' are confirmed. Total paid: " + money(event.totalAmount().toPlainString()) + ".");
            case EXPIRED -> new Notification(
                    "Booking " + ref + " expired",
                    "Your booking for '" + title + "' was not paid in time and has expired. The tickets were released.");
            case CANCELLED -> new Notification(
                    "Booking " + ref + " cancelled",
                    (event.cancellationReason() == CancellationReason.EVENT_CANCELLED
                            ? "The organizer cancelled '" + title + "'. "
                            : "Your booking for '" + title + "' was cancelled at your request. ")
                            + "Refund: " + money(event.refundAmount() == null ? "0.00" : event.refundAmount().toPlainString()) + ".");
            case PENDING_PAYMENT -> new Notification("Booking " + ref + " created", "Please pay before the hold expires.");
        };
    }

    private static String money(String amount) {
        return amount + " " + Booking.CURRENCY;
    }

    private record Notification(String subject, String message) {
    }
}
