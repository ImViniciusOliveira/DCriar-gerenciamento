package com.dcriar.exception.custom;

/**
 * Exceção lançada quando ocorre um erro ao tentar mesclar (merge) um JSON parcial
 * sobre um objeto DTO durante uma operação de PATCH.
 * <p>
 * Este erro geralmente encapsula uma {@link com.fasterxml.jackson.core.JsonProcessingException}
 * e indica um problema interno do servidor, resultando em uma resposta HTTP 500.
 */
public class JsonMergeException extends RuntimeException {

    public JsonMergeException(String message, Throwable cause) {
        super(message, cause);
    }
}
