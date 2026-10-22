package com.tazkara.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;


public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";
    public static final String INTERNAL_ROLE = "INTERNAL_SERVICE";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private final byte[] expectedKey;

    public InternalApiKeyFilter(String internalApiKey) {
        this.expectedKey = internalApiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided != null && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "internal-service", null, List.of(new SimpleGrantedAuthority("ROLE_" + INTERNAL_ROLE)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
