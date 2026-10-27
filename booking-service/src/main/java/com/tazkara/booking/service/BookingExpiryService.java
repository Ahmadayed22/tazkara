package com.tazkara.booking.service;

import com.tazkara.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Expires one booking in its own short transaction. The row is locked first (SELECT ... FOR UPDATE), so if a
 * payment is running on the same booking, one waits for the other and then re-checks the status: either the
 * payment confirmed it (the job does nothing) or the job expired it (the payment is rejected).
 */
@Service
public class BookingExpiryService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryService.class);

    private final BookingRepository bookingRepository;
    private final TicketReleaseService ticketReleaseService;
    private final Clock clock;

    public BookingExpiryService(BookingRepository bookingRepository, TicketReleaseService ticketReleaseService,
                                Clock clock) {
        this.bookingRepository = bookingRepository;
        this.ticketReleaseService = ticketReleaseService;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireIfOverdue(Long bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
                .filter(booking -> booking.expireIfOverdue(clock.instant()))
                .map(booking -> {
                    ticketReleaseService.release(booking, "HOLD_EXPIRED");
                    bookingRepository.save(booking);
                    log.info("Booking {} expired, tickets released", booking.getReference());
                    return true;
                })
                .orElse(false);
    }
}
