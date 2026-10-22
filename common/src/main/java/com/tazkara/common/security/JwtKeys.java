package com.tazkara.common.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;


public final class JwtKeys {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";

    private JwtKeys() {
    }

    public static SecretKey hmacKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
