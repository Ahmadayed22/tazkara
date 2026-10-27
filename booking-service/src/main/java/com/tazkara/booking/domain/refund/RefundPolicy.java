package com.tazkara.booking.domain.refund;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Strategy pattern: one implementation per refund rule. Adding a rule (e.g. 75% between 3 and 7 days)
 * means adding one class; neither the booking nor the resolver has to change.
 */
public interface RefundPolicy {

    /** Does this rule apply when the event starts in {@code timeBeforeStart}? (negative = already started) */
    boolean appliesTo(Duration timeBeforeStart);

    /** False when a confirmed booking may not be cancelled at all under this rule. */
    boolean allowsCancellation();

    /** Amount given back for a confirmed booking that cost {@code amountPaid}. */
    BigDecimal refundFor(BigDecimal amountPaid);

    /** Human readable rule, used in error messages. */
    String description();
}
