package com.tazkara.event.repository;

import com.tazkara.event.domain.Event;
import com.tazkara.event.domain.EventStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

/**
 * Small composable query pieces for the public event search; null filters are simply skipped,
 * which is what makes city, date range and keyword combinable in any way.
 */
public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> publiclyVisible(Instant now) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), EventStatus.PUBLISHED),
                cb.greaterThan(root.get("startsAt"), now));
    }

    public static Specification<Event> inCity(String city) {
        if (city == null || city.isBlank()) {
            return null;
        }
        String normalized = city.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), normalized);
    }

    public static Specification<Event> startsFrom(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startsAt"), from);
    }

    public static Specification<Event> startsBefore(Instant toExclusive) {
        return toExclusive == null ? null : (root, query, cb) -> cb.lessThan(root.get("startsAt"), toExclusive);
    }

    public static Specification<Event> titleContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\');
    }
}
