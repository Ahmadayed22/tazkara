package com.tazkara.identity.domain;
import com.tazkara.common.security.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class UserFactory {

    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserFactory(PasswordEncoder passwordEncoder, Clock clock) {
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public User create(String fullName, String email, String rawPassword, Role role, String organizationName) {
        return create( null,fullName, email, rawPassword, role, organizationName);
    }

    public User create(Long id, String fullName, String email, String rawPassword, Role role, String organizationName) {
        String hash = passwordEncoder.encode(rawPassword);
        return switch (role) {
            case ATTENDEE -> new User(id, fullName, EmailAddress.of(email), hash, Role.ATTENDEE, null, clock.instant());
            case ORGANIZER -> new User(id, fullName, EmailAddress.of(email), hash, Role.ORGANIZER,
                    organizationName, clock.instant());
        };
    }
}
