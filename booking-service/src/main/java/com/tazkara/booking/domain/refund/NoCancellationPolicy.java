package com.tazkara.booking.domain.refund;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/** Less than 48 hours before the start (or after it): a confirmed booking can no longer be cancelled. */
@Component
@Order(3)
public class NoCancellationPolicy implements RefundPolicy {

    @Override
    public boolean appliesTo(Duration timeBeforeStart) {
        return timeBeforeStart.compareTo(HalfRefundPolicy.THRESHOLD) < 0;
    }

    @Override
    public boolean allowsCancellation() {
        return false;
    }

    @Override
    public BigDecimal refundFor(BigDecimal amountPaid) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public String description() {
        return "Confirmed bookings cannot be cancelled less than 48 hours before the event";
    }
}
