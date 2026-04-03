package com.dcriar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS (Cross-Origin Resource Sharing) para a aplicação.
 * Lê a origem(s) permitida(s) a partir da propriedade `cors.allowed-origin`
 * (definida em `application-*.yml` ou por variável de ambiente `CORS_ALLOWED_ORIGIN`).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Injeta o valor da variável de ambiente/application.properties.
    // O nome da propriedade 'cors.allowed-origin' é o equivalente em kebab-case
    // da variável de ambiente 'CORS_ALLOWED_ORIGIN'.
    @Value("${cors.allowed-origin}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Aplica a configuração de CORS a todos os endpoints da aplicação.
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
}
