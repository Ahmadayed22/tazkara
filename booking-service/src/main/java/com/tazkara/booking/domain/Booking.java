
package com.tazkara.booking.domain;

import com.tazkara.booking.exception.BookingErrorCode;
import com.tazkara.common.error.DomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_bookings_attendee", columnList = "attendee_id, created_at"),
        @Index(name = "idx_bookings_event_status", columnList = "event_id, status"),
        @Index(name = "idx_bookings_status_hold", columnList = "status, hold_expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends AbstractAggregateRoot<Booking> {

    public static final String CURRENCY = "JOD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private BookingReference reference;

    @Column(name = "attendee_id", nullable = false)
    private UUID attendeeId;

    @Column(name = "attendee_email", nullable = false, length = 254)
    private String attendeeEmail;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "event_title", nullable = false, length = 150)
    private String eventTitle;

    @Column(name = "event_starts_at", nullable = false)
    private Instant eventStartsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Getter(AccessLevel.NONE)
    @ElementCollection
    @CollectionTable(
            name = "booking_lines",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @BatchSize(size = 50)
    private List<BookingLine> lines = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 20)
    private CancellationReason cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    /**
     * Factory method for a new booking.
     * A free booking is confirmed immediately.
     */
    public static Booking place(
            BookingReference reference,
            UUID attendeeId,
            String attendeeEmail,
            Long eventId,
            String eventTitle,
            Instant eventStartsAt,
            List<BookingLine> lines,
            Duration holdDuration,
            Instant now
    ) {
        if (lines.isEmpty() || lines.stream().anyMatch(line -> line.quantity() < 1)) {
            throw new IllegalArgumentException("A booking needs at least one ticket");
        }

        Booking booking = new Booking();
        booking.reference = reference;
        booking.attendeeId = attendeeId;
        booking.attendeeEmail = attendeeEmail;
        booking.eventId = eventId;
        booking.eventTitle = eventTitle;
        booking.eventStartsAt = eventStartsAt;
        booking.lines.addAll(lines);

        booking.totalAmount = lines.stream()
                .map(BookingLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        booking.createdAt = now;
        booking.status = BookingStatus.PENDING_PAYMENT;
        booking.holdExpiresAt = now.plus(holdDuration);

        if (booking.isFree()) {
            booking.transitionTo(BookingStatus.CONFIRMED);
            booking.confirmedAt = now;
            booking.holdExpiresAt = null;
            booking.raiseStatusChanged(now);
        }

        return booking;
    }

    public void ensurePayable(Instant now) {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new DomainException(
                    BookingErrorCode.BOOKING_NOT_PENDING,
                    "Booking " + reference + " is " + status + " and cannot be paid"
            );
        }

        if (isHoldOver(now)) {
            throw new DomainException(
                    BookingErrorCode.HOLD_EXPIRED,
                    "The payment window of booking " + reference + " has passed"
            );
        }
    }

    /** Simulated payment, always successful. */
    public void confirmPayment(PaymentMethod method, Instant now) {
        ensurePayable(now);
        transitionTo(BookingStatus.CONFIRMED);
        this.paymentMethod = method;
        this.paidAt = now;
        this.confirmedAt = now;
        raiseStatusChanged(now);
    }

    /** Called by the expiry job. */
    public boolean expireIfOverdue(Instant now) {
        if (status != BookingStatus.PENDING_PAYMENT || !isHoldOver(now)) {
            return false;
        }

        transitionTo(BookingStatus.EXPIRED);
        this.expiredAt = now;
        raiseStatusChanged(now);
        return true;
    }



    /** The organizer cancelled the event. */
    public void cancelBecauseEventCancelled(Instant now) {
        if (!BookingStatus.ACTIVE.contains(status)) {
            return;
        }

        this.refundAmount = status == BookingStatus.CONFIRMED
                ? totalAmount
                : BigDecimal.ZERO.setScale(2);

        cancel(CancellationReason.EVENT_CANCELLED, now);
    }

    public boolean isOwnedBy(UUID userId) {
        return attendeeId.equals(userId);
    }

    public boolean isFree() {
        return totalAmount.signum() == 0;
    }

    public int ticketCount() {
        return lines.stream().mapToInt(BookingLine::quantity).sum();
    }

    private boolean isHoldOver(Instant now) {
        return holdExpiresAt != null && !now.isBefore(holdExpiresAt);
    }

    private void cancel(CancellationReason reason, Instant now) {
        transitionTo(BookingStatus.CANCELLED);
        this.cancellationReason = reason;
        this.cancelledAt = now;
        raiseStatusChanged(now);
    }

    private void transitionTo(BookingStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new DomainException(
                    BookingErrorCode.INVALID_STATUS_TRANSITION,
                    "Booking cannot go from " + status + " to " + target
            );
        }

        this.status = target;
    }

    private void raiseStatusChanged(Instant now) {
        registerEvent(new BookingStatusChangedEvent(
                reference.value(),
                attendeeEmail,
                eventId,
                eventTitle,
                status,
                cancellationReason,
                totalAmount,
                refundAmount,
                now
        ));
    }

    /** Returns a read-only view of the booking lines. */
    public List<BookingLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
