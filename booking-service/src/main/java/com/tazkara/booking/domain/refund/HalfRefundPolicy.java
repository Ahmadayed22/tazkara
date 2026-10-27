package com.tazkara.booking.domain.refund;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/** From 48 hours up to (not including) 7 days before the start: 50% refund, rounded half-up to cents. */
@Component
@Order(2)
public class HalfRefundPolicy implements RefundPolicy {

    static final Duration THRESHOLD = Duration.ofHours(48);
    private static final BigDecimal HALF = new BigDecimal("0.50");

    @Override
    public boolean appliesTo(Duration timeBeforeStart) {
        return timeBeforeStart.compareTo(THRESHOLD) >= 0 && timeBeforeStart.compareTo(FullRefundPolicy.THRESHOLD) < 0;
    }

    @Override
    public boolean allowsCancellation() {
        return true;
    }

    @Override
    public BigDecimal refundFor(BigDecimal amountPaid) {
        return amountPaid.multiply(HALF).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String description() {
        return "50% refund when cancelled between 48 hours and 7 days before the event";
    }
}
