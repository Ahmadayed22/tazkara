package com.tazkara.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazkara.common.error.ApiError;
import com.tazkara.common.error.CommonErrorCode;
import com.tazkara.common.error.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Component
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        ApiError.Builder builder = ApiError.builder();
        if (request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE) instanceof DomainException tokenError) {
            builder.errorCode(tokenError.getErrorCode()).message(tokenError.getMessage());
        } else {
            builder.errorCode(CommonErrorCode.UNAUTHORIZED).message("Authentication is required");
        }
        write(response, builder.path(request.getRequestURI()).build());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, ApiError.builder()
                .errorCode(CommonErrorCode.ACCESS_DENIED)
                .message("You do not have permission to perform this action")
                .path(request.getRequestURI())
                .build());
    }

    private void write(HttpServletResponse response, ApiError error) throws IOException {
        response.setStatus(error.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
