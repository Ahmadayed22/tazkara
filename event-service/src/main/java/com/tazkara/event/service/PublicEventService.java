package com.tazkara.event.service;

import com.tazkara.common.error.DomainException;
import com.tazkara.common.web.PageResponse;
import com.tazkara.event.domain.Event;
import com.tazkara.event.exception.EventErrorCode;
import com.tazkara.event.repository.EventRepository;
import com.tazkara.event.repository.EventSpecifications;
import com.tazkara.event.dto.PublicEventResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Stream;

/**
 * Public browsing (US-07). Only published events that have not started are visible; everything else is 404.
 */
@Service
public class PublicEventService {

    private final EventRepository eventRepository;
    private final Clock clock;

    public PublicEventService(EventRepository eventRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    /**
     * @param fromDate first day included (UTC), optional
     * @param toDate   last day included (UTC), optional
     */
    @Transactional(readOnly = true)
    public PageResponse<PublicEventResponse> browse(String city, LocalDate fromDate, LocalDate toDate, String keyword,
                                                    int page, int size) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new DomainException(EventErrorCode.INVALID_DATE_RANGE, "'to' must not be before 'from'");
        }
        Instant from = fromDate == null ? null : fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Specification<Event> filter = Specification.allOf(Stream.of(
                        EventSpecifications.publiclyVisible(clock.instant()),
                        EventSpecifications.inCity(city),
                        EventSpecifications.startsFrom(from),
                        EventSpecifications.startsBefore(toExclusive),
                        EventSpecifications.titleContains(keyword))
                .filter(java.util.Objects::nonNull)
                .toList());

        PageRequest pageable = PageRequest.of(page, size, Sort.by("startsAt").ascending().and(Sort.by("id")));
        return PageResponse.of(eventRepository.findAll(filter, pageable), PublicEventResponse::summary);
    }

    @Transactional(readOnly = true)
    public PublicEventResponse details(Long eventId) {
        Instant now = clock.instant();
        return eventRepository.findById(eventId)
                .filter(event -> event.isPubliclyVisible(now))
                .map(PublicEventResponse::details)
                .orElseThrow(() -> new DomainException(EventErrorCode.EVENT_NOT_FOUND, "Event " + eventId + " not found"));
    }
}
