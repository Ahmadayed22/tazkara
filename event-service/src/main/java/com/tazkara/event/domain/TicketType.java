
package com.tazkara.event.domain;

import com.tazkara.common.error.DomainException;
import com.tazkara.event.exception.EventErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

/**
 * A ticket category of an event (Standard, VIP...).
 * available = quantity - reserved.
 */
@Entity
@Table(
    name = "ticket_types",
    check = {
        @CheckConstraint(
            name = "ck_ticket_types_reserved",
            constraint = "reserved >= 0 AND reserved <= quantity"
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int reserved;

    TicketType(Event event, TicketTypeSpec spec) {
        this.event = event;
        this.name = spec.name();
        this.price = spec.price();
        this.quantity = spec.quantity();
        this.reserved = 0;
    }

    public int available() {
        return quantity - reserved;
    }

    public boolean isSoldOut() {
        return available() == 0;
    }

    boolean canReserve(int requested) {
        return requested > 0 && requested <= available();
    }

    void reserve(int requested) {
        if (!canReserve(requested)) {
            throw new DomainException(
                    EventErrorCode.TICKETS_UNAVAILABLE,
                    "Only " + available() + " ticket(s) left for '" + name + "'"
            );
        }
        reserved += requested;
    }

    void release(int count) {
        // Never go below zero if a release is received twice.
        reserved = Math.max(0, reserved - count);
    }
}
