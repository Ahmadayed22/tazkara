package com.tazkara.event.config;


import com.tazkara.common.security.InternalApiKeyFilter;
import com.tazkara.common.security.Role;
import com.tazkara.common.security.StatelessSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, StatelessSecurity statelessSecurity) throws Exception {
        statelessSecurity.applyDefaults(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(StatelessSecurity.PUBLIC_DOCS).permitAll()
                        .requestMatchers("/internal/**").hasRole(InternalApiKeyFilter.INTERNAL_ROLE)
                        .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/*").permitAll()
                        .requestMatchers("/api/events/**", "/api/organizer/**").hasRole(Role.ORGANIZER.name())
                        .anyRequest().authenticated());
        return http.build();
    }
}
