package com.tazkara.identity.domain;
import com.tazkara.common.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;
@Component
public class UserFactory {

    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserFactory(PasswordEncoder passwordEncoder, Clock clock) {
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public User create(String fullName, String email, String rawPassword, Role role, String organizationName) {
        return create(UUID.randomUUID(), fullName, email, rawPassword, role, organizationName);
    }

    /** Variant with a fixed id, used for demo data so other services can reference the demo organizer. */
    public User create(UUID id, String fullName, String email, String rawPassword, Role role, String organizationName) {
        String hash = passwordEncoder.encode(rawPassword);
        return switch (role) {
            case ATTENDEE -> new User(id, fullName, EmailAddress.of(email), hash, Role.ATTENDEE, null, clock.instant());
            case ORGANIZER -> new User(id, fullName, EmailAddress.of(email), hash, Role.ORGANIZER,
                    organizationName, clock.instant());
        };
    }
}
