package com.dcriar.exception.custom;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import lombok.Getter;

/**
 * Exceção lançada quando um arquivo esperado não é encontrado no sistema de armazenamento.
 * <p>
 * A anotação {@code @ResponseStatus(HttpStatus.NOT_FOUND)} instrui o Spring a retornar
 * automaticamente o status HTTP 404 Not Found quando esta exceção é lançada
 * por um controller e não é capturada por um handler específico.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
@Getter
public class ArquivoNaoEncontradoException extends RuntimeException {

    private final String nomeArquivo;

    /**
     * Constrói a exceção com uma mensagem de erro.
     *
     * @param message A mensagem detalhando o erro.
     */
    private ArquivoNaoEncontradoException(String message, String nomeArquivo) {
        super(message);
        this.nomeArquivo = nomeArquivo;
    }

    /**
     * Constrói a exceção com uma mensagem de erro e a causa original.
     *
     * @param message A mensagem detalhando o erro.
     * @param cause   A exceção original que causou este erro.
     */
    public ArquivoNaoEncontradoException(String message, Throwable cause) {
        super(message, cause);
        this.nomeArquivo = null;
    }

    public static ArquivoNaoEncontradoException noStorage(String fileName) {
        return new ArquivoNaoEncontradoException("Arquivo não encontrado no storage: " + fileName, fileName);
    }
}
