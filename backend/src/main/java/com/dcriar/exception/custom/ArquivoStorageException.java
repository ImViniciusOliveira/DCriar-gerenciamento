package com.dcriar.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando ocorre um erro durante as operações de armazenamento de arquivos,
 * como salvar, carregar ou excluir.
 * <p>
 * Esta exceção é mapeada para um status HTTP 500 (Internal Server Error) pelo GlobalExceptionHandler,
 * indicando uma falha inesperada no backend durante o manuseio de arquivos.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ArquivoStorageException extends RuntimeException {

    /**
     * Construtor que aceita uma mensagem de erro.
     *
     * @param message A mensagem detalhando a causa da exceção.
     */
    public ArquivoStorageException(String message) {
        super(message);
    }

    /**
     * Construtor que aceita uma mensagem de erro e a causa original.
     *
     * @param message A mensagem detalhando a causa da exceção.
     * @param cause A exceção original que causou o erro de armazenamento.
     */
    public ArquivoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
