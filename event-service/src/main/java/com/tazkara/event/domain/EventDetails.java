package com.tazkara.event.domain;

import java.time.Instant;

/**
 * Value object grouping the descriptive data of an event (what an organizer types in the form).
 */
public record EventDetails(String title, String description, String venue, String city, Instant startsAt) {

    public EventDetails {
        title = title.trim();
        description = description.trim();
        venue = venue.trim();
        city = city.trim();
    }
}
