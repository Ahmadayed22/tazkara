package com.tazkara.event.service;

import com.tazkara.common.error.DomainException;

import com.tazkara.common.security.AuthenticatedUser;
import com.tazkara.common.web.PageResponse;
import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.EventStatus;
import com.tazkara.event.exception.EventErrorCode;
import com.tazkara.event.repository.EventRepository;
import com.tazkara.event.dto.EventRequest;
import com.tazkara.event.dto.OrganizerEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;


@Service
public class OrganizerEventService {

    private static final Logger log = LoggerFactory.getLogger(OrganizerEventService.class);

    private final EventRepository eventRepository;
 
    private final Clock clock;

    public OrganizerEventService(EventRepository eventRepository, Clock clock) {
        this.eventRepository = eventRepository;
     
        this.clock = clock;
    }

    @Transactional
    public OrganizerEventResponse create(AuthenticatedUser organizer, EventRequest request) {
        Event event = Event.createDraft(organizer.id(), request.toDetails(), request.toSpecs(), clock.instant());
        eventRepository.save(event);
        log.info("Organizer {} created draft event {}", organizer.id(), event.getId());
        return OrganizerEventResponse.from(event);
    }

    @Transactional
    public OrganizerEventResponse updateDraft(AuthenticatedUser organizer, Long eventId, EventRequest request) {
        Event event = loadOwnedForUpdate(organizer, eventId);
        event.updateDraft(request.toDetails(), request.toSpecs(), clock.instant());
        eventRepository.flush();
        return OrganizerEventResponse.from(event);
    }

    @Transactional
    public OrganizerEventResponse publish(AuthenticatedUser organizer, Long eventId) {
        Event event = loadOwnedForUpdate(organizer, eventId);
        event.publish(clock.instant());
        log.info("Event {} published", eventId);
        return OrganizerEventResponse.from(event);
    }



    @Transactional(readOnly = true)
    public OrganizerEventResponse getOwn(AuthenticatedUser organizer, Long eventId) {
        return eventRepository.findById(eventId)
                .filter(event -> event.isOwnedBy(organizer.id()))
                .map(OrganizerEventResponse::from)
                .orElseThrow(() -> notFound(eventId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrganizerEventResponse> listOwn(AuthenticatedUser organizer, EventStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Event> events = status == null
                ? eventRepository.findByOrganizerId(organizer.id(), pageable)
                : eventRepository.findByOrganizerIdAndStatus(organizer.id(), status, pageable);
        return PageResponse.of(events, OrganizerEventResponse::from);
    }

    private Event loadOwnedForUpdate(AuthenticatedUser organizer, Long eventId) {
        return eventRepository.findByIdForUpdate(eventId)
                .filter(event -> event.isOwnedBy(organizer.id()))
                .orElseThrow(() -> notFound(eventId));
    }

    static DomainException notFound(Long eventId) {
        return new DomainException(EventErrorCode.EVENT_NOT_FOUND, "Event " + eventId + " not found");
    }
}
