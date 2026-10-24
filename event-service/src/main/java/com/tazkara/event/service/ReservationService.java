package com.tazkara.event.service;

import com.tazkara.common.error.DomainException;
import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.TicketReservation;
import com.tazkara.event.exception.EventErrorCode;
import com.tazkara.event.repository.EventRepository;
import com.tazkara.event.repository.TicketReservationRepository;
import com.tazkara.event.dto.InternalDtos.EventSnapshot;
import com.tazkara.event.dto.InternalDtos.ReservationResponse;
import com.tazkara.event.dto.InternalDtos.ReservationResponse.ReservedLine;
import com.tazkara.event.dto.InternalDtos.ReserveTicketsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Ticket inventory operations requested by the Booking Service.
 * <p>
 * Overselling prevention: the event row is locked with SELECT ... FOR UPDATE, then the aggregate checks
 * availability of every line before changing any counter (all or nothing). Two concurrent requests for
 * the last ticket are serialised by the lock; the second one sees 0 available and gets TICKETS_UNAVAILABLE.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final EventRepository eventRepository;
    private final TicketReservationRepository reservationRepository;
    private final Clock clock;

    public ReservationService(EventRepository eventRepository, TicketReservationRepository reservationRepository,
                              Clock clock) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EventSnapshot snapshot(Long eventId) {
        return eventRepository.findById(eventId)
                .map(EventSnapshot::from)
                .orElseThrow(() -> OrganizerEventService.notFound(eventId));
    }

    @Transactional
    public ReservationResponse reserve(Long eventId, ReserveTicketsRequest request) {
        Map<Long, Integer> quantities = request.lines().stream()
                .collect(Collectors.toMap(ReserveTicketsRequest.Line::ticketTypeId,
                        ReserveTicketsRequest.Line::quantity, Integer::sum, LinkedHashMap::new));

        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> OrganizerEventService.notFound(eventId));

        Optional<TicketReservation> existing = reservationRepository.findByBookingReference(request.bookingReference());
        if (existing.isPresent()) {
            // Idempotent retry of the same reservation, or a reservation that was already released/compensated.
            TicketReservation reservation = existing.get();
            if (!reservation.isActive() || !eventId.equals(reservation.getEventId())) {
                throw new DomainException(EventErrorCode.RESERVATION_ALREADY_RELEASED,
                        "Reservation " + request.bookingReference() + " can no longer be used");
            }
            return toResponse(request.bookingReference(), event, reservation.quantitiesByTicketType());
        }

        Instant now = clock.instant();
        event.reserve(quantities, now);
        reservationRepository.save(TicketReservation.active(request.bookingReference(), eventId, quantities, now));
        log.info("Reserved {} for booking {} on event {}", quantities, request.bookingReference(), eventId);
        return toResponse(request.bookingReference(), event, quantities);
    }

    /** Idempotent: releasing twice, or releasing something never reserved, changes nothing. */
    @Transactional
    public void release(String bookingReference, Long eventId) {
        Instant now = clock.instant();
        Optional<TicketReservation> found = reservationRepository.findByBookingReference(bookingReference);
        if (found.isEmpty()) {
            reservationRepository.save(TicketReservation.tombstone(bookingReference, eventId, now));
            log.info("Release for unknown reservation {} recorded as tombstone", bookingReference);
            return;
        }
        TicketReservation reservation = found.get();
        if (!reservation.isActive()) {
            log.debug("Reservation {} already released, ignoring duplicate", bookingReference);
            return;
        }
        eventRepository.findByIdForUpdate(reservation.getEventId())
                .ifPresent(event -> event.release(reservation.quantitiesByTicketType()));
        reservation.markReleased(now);
        log.info("Released reservation {} on event {}", bookingReference, reservation.getEventId());
    }

    private static ReservationResponse toResponse(String reference, Event event, Map<Long, Integer> quantities) {
        List<ReservedLine> lines = quantities.entrySet().stream()
                .map(entry -> event.findTicketType(entry.getKey())
                        .map(type -> new ReservedLine(type.getId(), type.getName(), type.getPrice(), entry.getValue()))
                        .orElseThrow())
                .toList();
        return new ReservationResponse(reference, event.getId(), event.getTitle(), event.getStartsAt(), lines);
    }
}
