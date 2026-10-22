package com.tazkara.common.security;


public enum Role {
    ATTENDEE,
    ORGANIZER;

    public String authority() {
        return "ROLE_" + name();
    }
}
