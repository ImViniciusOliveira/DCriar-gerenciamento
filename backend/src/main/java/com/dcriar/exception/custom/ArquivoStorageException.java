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

    private final String operacao;
    private final String nomeArquivo;

    /**
     * Construtor que aceita uma mensagem de erro.
     *
     * @param message A mensagem detalhando a causa da exceção.
     */
    private ArquivoStorageException(String message, String operacao, String nomeArquivo) {
        super(message);
        this.operacao = operacao;
        this.nomeArquivo = nomeArquivo;
    }

    /**
     * Construtor que aceita uma mensagem de erro e a causa original.
     *
     * @param message A mensagem detalhando a causa da exceção.
     * @param cause A exceção original que causou o erro de armazenamento.
     */
    private ArquivoStorageException(String message, Throwable cause, String operacao, String nomeArquivo) {
        super(message, cause);
        this.operacao = operacao;
        this.nomeArquivo = nomeArquivo;
    }

    public static ArquivoStorageException falhaAoArmazenar(String originalName, Throwable cause) {
        return new ArquivoStorageException(
                String.format(
                        "Não foi possível armazenar o arquivo '%s'. Verifique o conteúdo enviado e tente novamente.",
                        originalName
                ),
                cause,
                "ARMAZENAR",
                originalName
        );
    }

    public String getOperacao() {
        return operacao;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }
}
