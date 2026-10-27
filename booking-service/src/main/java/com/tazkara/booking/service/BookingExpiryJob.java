package com.tazkara.booking.service;

import com.tazkara.booking.config.BookingProperties;
import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * US-12 background process. Runs on a thread of the Spring scheduler pool ("booking-scheduler-*"),
 * never on an HTTP thread. fixedDelay means the next run starts only after the previous one finished,
 * so two runs never overlap. Interval and hold duration are configurable.
 */
@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingRepository bookingRepository;
    private final BookingExpiryService expiryService;
    private final BookingProperties properties;
    private final Clock clock;

    public BookingExpiryJob(BookingRepository bookingRepository, BookingExpiryService expiryService,
                            BookingProperties properties, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.expiryService = expiryService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${tazkara.booking.expiry-job-interval:PT30S}", initialDelayString = "PT15S")
    public void expireOverdueBookings() {
        List<Long> overdue = bookingRepository.findOverdueIds(BookingStatus.PENDING_PAYMENT, clock.instant(),
                PageRequest.of(0, properties.expiryBatchSize()));
        if (overdue.isEmpty()) {
            return;
        }
        long expired = overdue.stream().filter(this::expireSafely).count();
        log.info("Expiry job: {} of {} overdue booking(s) expired", expired, overdue.size());
    }

    private boolean expireSafely(Long bookingId) {
        try {
            return expiryService.expireIfOverdue(bookingId);
        } catch (RuntimeException ex) {
            // One bad booking must not stop the others; it is retried on the next run.
            log.warn("Could not expire booking id={}: {}", bookingId, ex.getMessage());
            return false;
        }
    }
}
