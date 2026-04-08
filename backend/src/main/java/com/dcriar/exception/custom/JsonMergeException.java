package com.dcriar.exception.custom;

/**
 * Exceção lançada quando ocorre um erro ao tentar mesclar (merge) um JSON parcial
 * sobre um objeto DTO durante uma operação de PATCH.
 * <p>
 * Este erro geralmente encapsula uma {@link com.fasterxml.jackson.core.JsonProcessingException}
 * e indica um problema interno do servidor, resultando em uma resposta HTTP 500.
 */
public class JsonMergeException extends RuntimeException {

    private final String recurso;
    private final String operacao;

    private JsonMergeException(String message, Throwable cause, String recurso, String operacao) {
        super(message, cause);
        this.recurso = recurso;
        this.operacao = operacao;
    }

    public static JsonMergeException falhaAoMesclarPatch(String recurso, Throwable cause) {
        return new JsonMergeException(
                String.format(
                        "Não foi possível aplicar a atualização parcial em '%s'. Revise os campos enviados e tente novamente.",
                        recurso
                ),
                cause,
                recurso,
                "PATCH"
        );
    }

    public String getRecurso() {
        return recurso;
    }

    public String getOperacao() {
        return operacao;
    }
}
