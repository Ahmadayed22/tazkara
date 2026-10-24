package com.tazkara.event.domain;
/**
 * Event lifecycle. Valid paths: DRAFT -> PUBLISHED -> CANCELLED. Nothing ever goes back.
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED;

    public boolean canTransitionTo(EventStatus target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED;
            case PUBLISHED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
