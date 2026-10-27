package com.tazkara.booking.service;

import com.tazkara.booking.client.EventClientDtos.EventSnapshot;
import com.tazkara.booking.client.EventClientDtos.ReservationResult;
import com.tazkara.booking.client.EventServiceClient;
import com.tazkara.booking.config.BookingProperties;
import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingLine;
import com.tazkara.booking.domain.BookingReference;
import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.domain.PaymentMethod;

import com.tazkara.booking.exception.BookingErrorCode;
import com.tazkara.booking.repository.BookingRepository;
import com.tazkara.booking.dto.BookingResponse;
import com.tazkara.booking.dto.PlaceBookingRequest;
import com.tazkara.common.error.DomainException;
import com.tazkara.common.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Attendee use cases that change a booking: place (US-10), pay (US-11) and cancel (US-14).
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final EventServiceClient eventServiceClient;
    private final BookingReferenceGenerator referenceGenerator;
    private final TicketReleaseService ticketReleaseService;
  
    private final BookingProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepository, EventServiceClient eventServiceClient,
                          BookingReferenceGenerator referenceGenerator, TicketReleaseService ticketReleaseService,
                           BookingProperties properties,
                          TransactionTemplate transactionTemplate, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.eventServiceClient = eventServiceClient;
        this.referenceGenerator = referenceGenerator;
        this.ticketReleaseService = ticketReleaseService;
       
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    /**
     * Places a booking:
     *
     *   lock (attendee, event) and check the 6-ticket limit against active bookings;
     *   reserve the tickets synchronously in the Event Service (all or nothing, never oversold);
     *   store the booking with the prices returned by the Event Service.
     * 
     * If step 3 fails after step 2 succeeded, or step 2 times out, a compensating release is queued so the
     * Event Service frees the tickets again.
     */
    @Transactional
    public BookingResponse place(AuthenticatedUser attendee, PlaceBookingRequest request) {
        Map<Long, Integer> quantities = request.quantitiesByTicketType();
        int requested = quantities.values().stream().mapToInt(Integer::intValue).sum();
        Long eventId = request.eventId();

        bookingRepository.lockAttendeeEvent(lockKey(attendee, eventId));
        long alreadyBooked = bookingRepository.countTickets(attendee.id(), eventId, BookingStatus.ACTIVE);
        if (alreadyBooked + requested > properties.maxTicketsPerEvent()) {
            throw new DomainException(BookingErrorCode.TICKET_LIMIT_EXCEEDED,
                    "At most " + properties.maxTicketsPerEvent() + " tickets per event; you already hold "
                            + alreadyBooked + " and requested " + requested);
        }

        BookingReference reference = referenceGenerator.next();
        ReservationResult reservation = reserveTickets(eventId, reference.value(), quantities);
        compensateIfNotCommitted(reference.value(), eventId);

        List<BookingLine> lines = reservation.lines().stream()
                .map(line -> new BookingLine(line.ticketTypeId(), line.ticketTypeName(), line.unitPrice(), line.quantity()))
                .toList();
        Booking booking = Booking.place(reference, attendee.id(), attendee.email(), eventId,
                reservation.eventTitle(), reservation.eventStartsAt(), lines, properties.holdDuration(), clock.instant());
        bookingRepository.save(booking);
        log.info("Booking {} placed by attendee {} for event {} ({} tickets, status {})",
                reference, attendee.id(), eventId, requested, booking.getStatus());
        return BookingResponse.from(booking);
    }

    /**
     * Simulated payment. The event is checked synchronously (it may have been cancelled a second ago);
     * then the booking row is locked so a concurrent expiry job or second payment waits and then sees
     * the new status: exactly one of them wins.
     */
    public BookingResponse pay(AuthenticatedUser attendee, String reference, PaymentMethod method) {
        Booking snapshot = bookingRepository.findByReference(reference)
                .filter(booking -> booking.isOwnedBy(attendee.id()))
                .orElseThrow(() -> notFound(reference));
        snapshot.ensurePayable(clock.instant());

        EventSnapshot event = eventServiceClient.getEvent(snapshot.getEventId());
        if (event.isCancelled()) {
            throw new DomainException(BookingErrorCode.EVENT_CANCELLED, "The event has been cancelled");
        }
        if (!event.startsAt().isAfter(clock.instant())) {
            throw new DomainException(BookingErrorCode.EVENT_ALREADY_STARTED, "The event has already started");
        }

        return transactionTemplate.execute(status -> {
            Booking booking = lockOwned(attendee, reference);
            booking.confirmPayment(method, clock.instant());
            bookingRepository.save(booking);
            log.info("Booking {} paid with {}", reference, method);
            return BookingResponse.from(booking);
        });
    }



    private ReservationResult reserveTickets(Long eventId, String reference, Map<Long, Integer> quantities) {
        try {
            return eventServiceClient.reserve(eventId, reference, quantities);
        } catch (DomainException ex) {
            if (ex.getErrorCode() == BookingErrorCode.EVENT_SERVICE_UNAVAILABLE) {
                // We do not know whether the Event Service reserved the tickets before failing: release to be safe.
                ticketReleaseService.compensate(reference, eventId);
            }
            throw ex;
        }
    }

    /** Tickets are reserved remotely; if our own transaction does not commit, give them back. */
    private void compensateIfNotCommitted(String reference, Long eventId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    log.warn("Booking {} was not stored, releasing its reservation", reference);
                    ticketReleaseService.compensate(reference, eventId);
                }
            }
        });
    }

    private Booking lockOwned(AuthenticatedUser attendee, String reference) {
        return bookingRepository.findByReferenceForUpdate(reference)
                .filter(booking -> booking.isOwnedBy(attendee.id()))
                .orElseThrow(() -> notFound(reference));
    }

    private static long lockKey(AuthenticatedUser attendee, Long eventId) {
        return ((long) attendee.id().hashCode() << 32) ^ eventId;
    }

    static DomainException notFound(String reference) {
        return new DomainException(BookingErrorCode.BOOKING_NOT_FOUND, "Booking " + reference + " not found");
    }
}
