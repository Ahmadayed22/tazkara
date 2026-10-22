package com.tazkara.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;


@Component
public class StatelessSecurity {

    public static final String[] PUBLIC_DOCS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**", "/error"
    };

    private final JwtTokenValidator jwtTokenValidator;
    private final SecurityProperties properties;
    private final SecurityErrorResponder errorResponder;

    public StatelessSecurity(JwtTokenValidator jwtTokenValidator, SecurityProperties properties,
                             SecurityErrorResponder errorResponder) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.properties = properties;
        this.errorResponder = errorResponder;
    }

    public HttpSecurity applyDefaults(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(errorResponder)
                        .accessDeniedHandler(errorResponder))
                .addFilterBefore(new InternalApiKeyFilter(properties.internalApiKey()),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenValidator),
                        UsernamePasswordAuthenticationFilter.class);
    }
}
