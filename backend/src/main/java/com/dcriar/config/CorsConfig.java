package com.dcriar.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS (Cross-Origin Resource Sharing) para a aplicação.
 * <p>
 * Esta classe é essencial para permitir que o frontend (rodando em http://localhost:4200)
 * se comunique com o backend (rodando em http://localhost:8080) durante o desenvolvimento,
 * resolvendo os erros de política de mesma origem do navegador.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**") // Aplica a configuração a todos os endpoints sob /api/v1/
                .allowedOrigins("http://localhost:4200") // Permite requisições desta origem (Frontend Angular)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD") // Métodos HTTP permitidos
                .allowCredentials(true); // Permite o envio de cookies e headers de autenticação
    }
}