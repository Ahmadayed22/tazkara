package com.tazkara.common.security;

import java.util.UUID;


public record AuthenticatedUser(Long id, String email, Role role) {

    public boolean isOrganizer() {
        return role == Role.ORGANIZER;
    }

    public boolean isAttendee() {
        return role == Role.ATTENDEE;
    }
}
