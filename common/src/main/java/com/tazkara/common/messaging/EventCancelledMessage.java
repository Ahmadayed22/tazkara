package com.tazkara.common.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;


@JsonIgnoreProperties(ignoreUnknown = true)
public record EventCancelledMessage(Long eventId, String eventTitle, Instant cancelledAt) {
}
