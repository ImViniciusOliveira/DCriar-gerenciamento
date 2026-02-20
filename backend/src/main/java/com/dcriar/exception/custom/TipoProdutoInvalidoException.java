package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação recebe um tipo de produto inválido.
 * <p>
 * Os tipos de produto válidos são: 'CORTE' e 'CONSUMO_DIRETO'.
 * Qualquer outro valor resulta nesta exceção.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 400 (Bad Request).
 */
@Getter
public class TipoProdutoInvalidoException extends RuntimeException {

    /**
     * O tipo de produto inválido que foi fornecido.
     */
    private final String tipoProdutoFornecido;

    /**
     * Constrói a exceção com o tipo de produto inválido.
     *
     * @param tipoProdutoFornecido O valor inválido fornecido como tipo de produto.
     */
    public TipoProdutoInvalidoException(String tipoProdutoFornecido) {
        super(String.format(
                "Tipo de produto inválido: '%s'. Os tipos válidos são: 'CORTE' ou 'CONSUMO_DIRETO'.",
                tipoProdutoFornecido
        ));
        this.tipoProdutoFornecido = tipoProdutoFornecido;
    }
}

