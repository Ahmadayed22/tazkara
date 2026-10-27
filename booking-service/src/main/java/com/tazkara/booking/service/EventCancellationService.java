package com.tazkara.booking.service;

import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * US-09 (booking side): when an event is cancelled, every PENDING_PAYMENT and CONFIRMED booking is cancelled
 * with reason EVENT_CANCELLED; confirmed ones record a full refund. Rows are locked so a payment running at
 * the same moment is serialised with this. Idempotent: a duplicate message finds nothing active left.
 * Tickets are not released, because a cancelled event can never be booked again.
 */
@Service
public class EventCancellationService {

    private static final Logger log = LoggerFactory.getLogger(EventCancellationService.class);

    private final BookingRepository bookingRepository;
    private final Clock clock;

    public EventCancellationService(BookingRepository bookingRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.clock = clock;
    }

    @Transactional
    public int cancelBookingsOfEvent(Long eventId) {
        Instant now = clock.instant();
        List<Booking> active = bookingRepository.findByEventIdAndStatusInForUpdate(eventId, BookingStatus.ACTIVE);
        active.forEach(booking -> {
            booking.cancelBecauseEventCancelled(now);
            // save() publishes the booking's domain event -> notification after commit.
            bookingRepository.save(booking);
        });
        log.info("Event {} cancelled: {} booking(s) cancelled", eventId, active.size());
        return active.size();
    }
}
