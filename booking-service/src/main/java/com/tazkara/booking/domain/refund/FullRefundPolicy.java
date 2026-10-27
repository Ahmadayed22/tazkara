package com.tazkara.booking.domain.refund;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/** 7 days or more before the start: 100% refund. */
@Component
@Order(1)
public class FullRefundPolicy implements RefundPolicy {

    static final Duration THRESHOLD = Duration.ofDays(7);

    @Override
    public boolean appliesTo(Duration timeBeforeStart) {
        return timeBeforeStart.compareTo(THRESHOLD) >= 0;
    }

    @Override
    public boolean allowsCancellation() {
        return true;
    }

    @Override
    public BigDecimal refundFor(BigDecimal amountPaid) {
        return amountPaid.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String description() {
        return "100% refund when cancelled 7 or more days before the event";
    }
}
