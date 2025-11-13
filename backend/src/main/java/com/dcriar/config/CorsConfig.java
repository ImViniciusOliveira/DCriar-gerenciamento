package com.dcriar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS (Cross-Origin Resource Sharing) para a aplicação.
 * Lê a origem(s) permitida(s) a partir da propriedade `cors.allowed-origins`
 * (definida em `application-*.yml` ou por variável de ambiente `CORS_ALLOWED_ORIGIN`).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Pode conter uma origem ou várias separadas por vírgula. Ex: "http://localhost,http://example.com"
    @Value("${cors.allowed-origins:*}")
    private String allowedOriginsProperty;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = allowedOriginsProperty == null || allowedOriginsProperty.isBlank()
                ? new String[]{"*"}
                : allowedOriginsProperty.split(" *, *");

        registry.addMapping("/api/v1/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowCredentials(true);
    }
}