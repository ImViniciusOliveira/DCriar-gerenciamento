package com.dcriar.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro leve para registrar metadados de requisições (method, URI, headers importantes).
 * Não registra corpo. Útil para diagnóstico quando o frontend faz chamadas inesperadas.
 * Nota: por padrão NÃO registra nada para evitar poluição de logs em desenvolvimento.
 * Para ativar temporariamente, envie o header HTTP: X-Debug-Requests: true
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Só registra quando explicitamente solicitado pelo cliente via header
            boolean clientWantsDebug = "true".equalsIgnoreCase(request.getHeader("X-Debug-Requests"));
            if (clientWantsDebug) {
                String method = request.getMethod();
                String uri = request.getRequestURI();
                String contentType = request.getContentType();
                long contentLength = request.getContentLengthLong();
                String origin = request.getHeader("Origin");
                String referer = request.getHeader("Referer");

                log.info("[REQ] method={} uri={} contentType={} contentLength={} origin={} referer={}",
                        method, uri, contentType, contentLength, origin, referer);
            }
        } catch (Exception e) {
            // nunca interromper a requisição por causa de falha de logging
            log.debug("[REQ] Falha ao logar metadata da requisição: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
