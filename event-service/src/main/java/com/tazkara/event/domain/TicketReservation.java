
package com.tazkara.event.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Remembers which tickets were reserved for which booking reference.
 * A release for the same reference is applied only once, and a release
 * received before reservation leaves a RELEASED tombstone.
 */
@Entity
@Table(
        name = "ticket_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_reference",
                columnNames = "booking_reference"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketReservation {

    public enum Status {
        ACTIVE, RELEASED
    }

    @Embeddable
    public record Line(
            @Column(name = "ticket_type_id", nullable = false)
            Long ticketTypeId,

            @Column(nullable = false)
            int quantity
    ) {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, length = 20)
    private String bookingReference;

    @Column(name = "event_id")
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ticket_reservation_lines",
            joinColumns = @JoinColumn(name = "reservation_id")
    )
    private List<Line> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Version
    private long version;

    public static TicketReservation active(
            String reference,
            Long eventId,
            Map<Long, Integer> quantities,
            Instant now
    ) {
        TicketReservation reservation = new TicketReservation();
        reservation.bookingReference = reference;
        reservation.eventId = eventId;
        reservation.status = Status.ACTIVE;
        reservation.createdAt = now;

        quantities.forEach((ticketTypeId, quantity) ->
                reservation.lines.add(new Line(ticketTypeId, quantity))
        );

        return reservation;
    }

    /**
     * A release received for a reference we never reserved:
     * blocks any later reservation with the same reference.
     */
    public static TicketReservation tombstone(
            String reference,
            Long eventId,
            Instant now
    ) {
        TicketReservation reservation = new TicketReservation();
        reservation.bookingReference = reference;
        reservation.eventId = eventId;
        reservation.status = Status.RELEASED;
        reservation.createdAt = now;
        reservation.releasedAt = now;

        return reservation;
    }

    public void markReleased(Instant now) {
        this.status = Status.RELEASED;
        this.releasedAt = now;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public Map<Long, Integer> quantitiesByTicketType() {
        return lines.stream()
                .collect(Collectors.toMap(
                        Line::ticketTypeId,
                        Line::quantity,
                        Integer::sum
                ));
    }
}
