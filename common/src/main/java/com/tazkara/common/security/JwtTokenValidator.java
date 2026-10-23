package com.tazkara.common.security;

import com.tazkara.common.error.CommonErrorCode;
import com.tazkara.common.error.DomainException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Date;


/**
 * Validates a JWT locally (signature, issuer, expiry) without calling the Identity Service.
 */
@Component
public class JwtTokenValidator {

    private final JwtParser parser;

    public JwtTokenValidator(SecurityProperties properties, Clock clock) {
        this.parser = Jwts.parser()
                .verifyWith(JwtKeys.hmacKey(properties.jwt().secret()))
                .requireIssuer(properties.jwt().issuer())
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    /**
     * @throws DomainException with TOKEN_EXPIRED or TOKEN_INVALID when the token cannot be trusted
     */
    public AuthenticatedUser validate(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            String email = claims.get(JwtKeys.CLAIM_EMAIL, String.class);
            String role = claims.get(JwtKeys.CLAIM_ROLE, String.class);
            if (subject == null || email == null || role == null || claims.getIssuedAt() == null) {
                throw invalid();
            }
            return new AuthenticatedUser(  Long.parseLong(subject), email, Role.valueOf(role));
        } catch (ExpiredJwtException ex) {
            throw new DomainException(CommonErrorCode.TOKEN_EXPIRED, "Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw invalid();
        }
    }

    private static DomainException invalid() {
        return new DomainException(CommonErrorCode.TOKEN_INVALID, "Token is invalid");
    }
}
