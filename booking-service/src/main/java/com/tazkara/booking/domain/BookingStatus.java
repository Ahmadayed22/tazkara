package com.tazkara.booking.domain;

import java.util.EnumSet;
import java.util.Set;


public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    EXPIRED,
    CANCELLED;

    /** Bookings that still hold or own tickets. */
    public static final Set<BookingStatus> ACTIVE = EnumSet.of(PENDING_PAYMENT, CONFIRMED);

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case PENDING_PAYMENT -> target == CONFIRMED || target == EXPIRED || target == CANCELLED;
            case CONFIRMED -> target == CANCELLED;
            case EXPIRED, CANCELLED -> false;
        };
    }
}
