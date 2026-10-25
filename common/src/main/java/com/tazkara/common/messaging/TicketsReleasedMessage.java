package com.tazkara.common.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Integration event: the tickets reserved under {@code bookingReference} are free again
 * (booking expired, cancelled by the attendee, or never stored because of a failure).
 * The Event Service releases the matching reservation; handling it twice has no effect.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketsReleasedMessage(String bookingReference, Long eventId, String reason, Instant releasedAt) {
}
