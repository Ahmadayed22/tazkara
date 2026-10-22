package com.tazkara.common.web;

import com.tazkara.common.security.SecurityProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Beans every service needs: a UTC clock (injected everywhere instead of calling Instant.now(), which keeps
 * time handling consistent and testable) and the OpenAPI definition with the JWT "Authorize" button.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class CommonWebConfiguration {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI openApi(@Value("${tazkara.openapi.title}") String title,
                           @Value("${tazkara.openapi.description}") String description) {
        return new OpenAPI()
                .info(new Info().title(title).description(description).version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste the accessToken returned by POST /api/auth/login on the Identity Service")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
