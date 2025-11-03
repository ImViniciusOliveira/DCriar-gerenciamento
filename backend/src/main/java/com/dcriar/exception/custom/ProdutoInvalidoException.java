package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Map;

/**
 * Exceção específica para múltiplos erros de validação de um {@link com.dcriar.domain.product.entity.Produto}.
 * <p>
 * Esta exceção agrega múltiplos erros de validação em uma única resposta, permitindo
 * que o cliente da API receba todos os problemas de uma só vez.
 * É tipicamente capturada por um handler que retorna uma resposta HTTP 400 (Bad Request).
 */
@Getter
public class ProdutoInvalidoException extends RuntimeException {

    /**
     * Um mapa contendo os erros de validação, onde a chave é o nome do campo
     * que falhou na validação e o valor é a mensagem de erro correspondente.
     */
    private final Map<String, String> errors;

    /**
     * Constrói a exceção com uma mensagem geral e o mapa de erros detalhados.
     *
     * @param message A mensagem geral que resume o erro de validação.
     * @param errors O mapa de erros específicos por campo do produto.
     */
    public ProdutoInvalidoException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}
