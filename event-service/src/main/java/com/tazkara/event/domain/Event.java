
package com.tazkara.event.domain;

import com.tazkara.common.error.DomainException;
import com.tazkara.event.exception.EventErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;



@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_status_starts_at", columnList = "status, starts_at"),
        @Index(name = "idx_events_organizer", columnList = "organizer_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    public static final Duration MIN_LEAD_TIME = Duration.ofHours(24);
    public static final int MAX_TICKET_TYPES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false, length = 150)
    private String venue;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<TicketType> ticketTypes = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    private long version;

    /** Factory method: a new event always starts as a DRAFT owned by its creator. */
    public static Event createDraft(
            Long organizerId,
            EventDetails details,
            List<TicketTypeSpec> specs,
            Instant now
    ) {
        Event event = new Event();
        event.organizerId = organizerId;
        event.status = EventStatus.DRAFT;
        event.createdAt = now;
        event.apply(details, specs, now);
        return event;
    }

    public void updateDraft(EventDetails details, List<TicketTypeSpec> specs, Instant now) {
        if (status != EventStatus.DRAFT) {
            throw new DomainException(
                    EventErrorCode.EVENT_NOT_EDITABLE,
                    "Only DRAFT events can be edited; this event is " + status
            );
        }
        apply(details, specs, now);
    }

    public void publish(Instant now) {
        transitionTo(EventStatus.PUBLISHED);
        requireLeadTime(startsAt, now);
        this.publishedAt = now;
    }

    public void cancel(Instant now) {
        transitionTo(EventStatus.CANCELLED);
        this.cancelledAt = now;
    }

    public boolean isOwnedBy(Long userId) {
        return organizerId.equals(userId);
    }

    /** Visible to the public = published and not started yet. */
    public boolean isPubliclyVisible(Instant now) {
        return status == EventStatus.PUBLISHED && startsAt.isAfter(now);
    }

    public boolean isSoldOut() {
        return ticketTypes.stream().allMatch(TicketType::isSoldOut);
    }

    public Optional<TicketType> findTicketType(Long ticketTypeId) {
        return ticketTypes.stream()
                .filter(t -> t.getId().equals(ticketTypeId))
                .findFirst();
    }

    /**
     * Reserves tickets, all or nothing.
     * The caller must hold a write lock on this event.
     */
    public void reserve(Map<Long, Integer> quantitiesByTicketType, Instant now) {
        if (!isPubliclyVisible(now)) {
            throw new DomainException(
                    EventErrorCode.EVENT_NOT_BOOKABLE,
                    "Event " + id + " is not open for booking"
            );
        }

        quantitiesByTicketType.forEach((ticketTypeId, quantity) -> {
            TicketType type = findTicketType(ticketTypeId).orElseThrow(() ->
                    new DomainException(
                            EventErrorCode.TICKET_TYPE_NOT_FOUND,
                            "Ticket type " + ticketTypeId + " does not belong to event " + id
                    )
            );

            if (!type.canReserve(quantity)) {
                throw new DomainException(
                        EventErrorCode.TICKETS_UNAVAILABLE,
                        "Not enough tickets available for '" + type.getName()
                                + "' (requested " + quantity
                                + ", available " + type.available() + ")"
                );
            }
        });

        quantitiesByTicketType.forEach((ticketTypeId, quantity) ->
                findTicketType(ticketTypeId).orElseThrow().reserve(quantity)
        );
    }

    public void release(Map<Long, Integer> quantitiesByTicketType) {
        quantitiesByTicketType.forEach((ticketTypeId, quantity) ->
                findTicketType(ticketTypeId)
                        .ifPresent(type -> type.release(quantity))
        );
    }

    private void apply(EventDetails details, List<TicketTypeSpec> specs, Instant now) {
        requireLeadTime(details.startsAt(), now);
        validateTicketTypes(specs);

        this.title = details.title();
        this.description = details.description();
        this.venue = details.venue();
        this.city = details.city();
        this.startsAt = details.startsAt();

        // Drafts never have reservations, so replacing the list is safe.
        this.ticketTypes.clear();
        specs.forEach(spec -> this.ticketTypes.add(new TicketType(this, spec)));
    }

    private void transitionTo(EventStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new DomainException(
                    EventErrorCode.INVALID_STATUS_TRANSITION,
                    "Event cannot go from " + status + " to " + target
            );
        }
        this.status = target;
    }

    private static void requireLeadTime(Instant startsAt, Instant now) {
        if (startsAt.isBefore(now.plus(MIN_LEAD_TIME))) {
            throw new DomainException(
                    EventErrorCode.EVENT_START_TOO_SOON,
                    "The event must start at least "
                            + MIN_LEAD_TIME.toHours() + " hours from now"
            );
        }
    }

    private static void validateTicketTypes(List<TicketTypeSpec> specs) {
        if (specs == null || specs.isEmpty() || specs.size() > MAX_TICKET_TYPES) {
            throw new DomainException(
                    EventErrorCode.INVALID_TICKET_TYPES,
                    "An event needs between 1 and " + MAX_TICKET_TYPES + " ticket types"
            );
        }

        Set<String> seen = new HashSet<>();
        specs.forEach(spec -> {
            if (!seen.add(spec.name().toLowerCase(Locale.ROOT))) {
                throw new DomainException(
                        EventErrorCode.DUPLICATE_TICKET_TYPE_NAME,
                        "Ticket type name '" + spec.name() + "' is used more than once"
                );
            }
        });
    }

    /**
     * Returns a read-only view to prevent callers from modifying the ticket types directly.
     */
    public List<TicketType> getTicketTypes() {
        return java.util.Collections.unmodifiableList(ticketTypes);
    }
}
