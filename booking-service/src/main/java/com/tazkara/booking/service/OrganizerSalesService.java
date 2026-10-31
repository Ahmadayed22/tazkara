package com.tazkara.booking.service;

import com.tazkara.booking.client.EventClientDtos.EventSnapshot;
import com.tazkara.booking.client.EventServiceClient;
import com.tazkara.booking.domain.Booking;
import com.tazkara.booking.domain.BookingStatus;
import com.tazkara.booking.exception.BookingErrorCode;
import com.tazkara.booking.repository.BookingRepository;
import com.tazkara.booking.repository.TicketSalesRow;
import com.tazkara.booking.service.csv.BookingCsvExporter;
import com.tazkara.booking.dto.OrganizerBookingResponse;
import com.tazkara.booking.dto.SalesSummaryResponse;
import com.tazkara.booking.dto.SalesSummaryResponse.TicketTypeSales;
import com.tazkara.common.error.DomainException;
import com.tazkara.common.security.AuthenticatedUser;
import com.tazkara.common.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Organizer use cases on the booking side: list bookings (US-15), sales summary (US-15) and CSV export (US-16).
 * Ownership is verified through the Event Service (synchronous call), because events are not our data.
 */
@Service
public class OrganizerSalesService {

    private final BookingRepository bookingRepository;
    private final EventServiceClient eventServiceClient;
    private final BookingCsvExporter csvExporter;
    private final Clock clock;

    public OrganizerSalesService(BookingRepository bookingRepository, EventServiceClient eventServiceClient,
                                 BookingCsvExporter csvExporter, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.eventServiceClient = eventServiceClient;
        this.csvExporter = csvExporter;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizerBookingResponse> listBookings(AuthenticatedUser organizer, Long eventId,
                                                               BookingStatus status, int page, int size) {
        requireOwnedEvent(organizer, eventId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Booking> bookings = status == null
                ? bookingRepository.findByEventId(eventId, pageable)
                : bookingRepository.findByEventIdAndStatus(eventId, status, pageable);
        return PageResponse.of(bookings, OrganizerBookingResponse::from);
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(AuthenticatedUser organizer, Long eventId) {
        EventSnapshot event = requireOwnedEvent(organizer, eventId);
        // ticketTypeId -> (status -> aggregated row)
        Map<Long, Map<BookingStatus, TicketSalesRow>> rows = bookingRepository
                .summarizeByTicketType(eventId, BookingStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(TicketSalesRow::ticketTypeId,
                        Collectors.toMap(TicketSalesRow::status, row -> row)));

        List<TicketTypeSales> perType = event.ticketTypes().stream().map(type -> {
            Map<BookingStatus, TicketSalesRow> byStatus = rows.getOrDefault(type.id(), Map.of());
            long sold = tickets(byStatus.get(BookingStatus.CONFIRMED));
            long held = tickets(byStatus.get(BookingStatus.PENDING_PAYMENT));
            BigDecimal revenue = amount(byStatus.get(BookingStatus.CONFIRMED));
            long remaining = Math.max(0, type.quantity() - sold - held);
            return new TicketTypeSales(type.id(), type.name(), type.price(), type.quantity(), sold, held, remaining, revenue);
        }).toList();

        return new SalesSummaryResponse(event.id(), event.title(), event.status(), Booking.CURRENCY, perType,
                perType.stream().mapToLong(TicketTypeSales::sold).sum(),
                perType.stream().mapToLong(TicketTypeSales::held).sum(),
                perType.stream().map(TicketTypeSales::revenue).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Transactional(readOnly = true)
    public CsvFile exportConfirmed(AuthenticatedUser organizer, Long eventId) {
        requireOwnedEvent(organizer, eventId);
        List<Booking> confirmed = bookingRepository.findWithLinesByEventIdAndStatus(eventId, BookingStatus.CONFIRMED);
        String fileName = "event-" + eventId + "-confirmed-bookings-" + LocalDate.now(clock.withZone(ZoneOffset.UTC)) + ".csv";
        return new CsvFile(fileName, csvExporter.export(confirmed));
    }

    private EventSnapshot requireOwnedEvent(AuthenticatedUser organizer, Long eventId) {
        EventSnapshot event = eventServiceClient.getEvent(eventId);
        if (!organizer.id().equals(event.organizerId())) {
            // Same answer as a missing event: knowing an id must not reveal someone else's event.
            throw new DomainException(BookingErrorCode.EVENT_NOT_FOUND, "Event " + eventId + " not found");
        }
        return event;
    }

    private static long tickets(TicketSalesRow row) {
        return row == null ? 0 : row.tickets();
    }

    private static BigDecimal amount(TicketSalesRow row) {
        return row == null ? BigDecimal.ZERO.setScale(2) : row.amount().setScale(2);
    }

    public record CsvFile(String fileName, byte[] content) {
    }
}
