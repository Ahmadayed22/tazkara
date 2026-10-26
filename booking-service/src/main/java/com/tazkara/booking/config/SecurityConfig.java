package com.tazkara.booking.config;

import com.tazkara.common.security.Role;
import com.tazkara.common.security.StatelessSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Every Booking Service endpoint needs a valid JWT: attendees for /api/bookings, organizers for /api/organizer. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, StatelessSecurity statelessSecurity) throws Exception {
        statelessSecurity.applyDefaults(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(StatelessSecurity.PUBLIC_DOCS).permitAll()
                        .requestMatchers("/api/bookings/**", "/api/bookings").hasRole(Role.ATTENDEE.name())
                        .requestMatchers("/api/organizer/**").hasRole(Role.ORGANIZER.name())
                        .anyRequest().authenticated());
        return http.build();
    }
}
