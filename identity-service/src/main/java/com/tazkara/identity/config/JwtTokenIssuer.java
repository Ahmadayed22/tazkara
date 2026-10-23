package com.tazkara.identity.config;

import com.tazkara.common.security.JwtKeys;
import com.tazkara.common.security.SecurityProperties;
import com.tazkara.identity.domain.User;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;


@Component
public class JwtTokenIssuer {

    private final SecretKey key;
    private final SecurityProperties.Jwt settings;
    private final Clock clock;

    public JwtTokenIssuer(SecurityProperties properties, Clock clock) {
        this.settings = properties.jwt();
        this.key = JwtKeys.hmacKey(settings.secret());
        this.clock = clock;
    }

    public IssuedToken issue(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(settings.expiration());
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .issuer(settings.issuer())
                .claim(JwtKeys.CLAIM_EMAIL, user.getEmail().value())
                .claim(JwtKeys.CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt, settings.expiration().toSeconds());
    }

    public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {
    }
}
