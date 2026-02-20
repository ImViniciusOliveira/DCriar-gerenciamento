package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação recebe um operador de estoque inválido.
 * <p>
 * Os operadores de estoque válidos são: 'GTE' (maior ou igual) e 'LTE' (menor ou igual).
 * Qualquer outro valor resulta nesta exceção.
 * <p>
 * Esta exceção é utilizada especialmente em operações de filtro de estoque,
 * onde o usuário especifica uma condição de comparação.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 400 (Bad Request).
 */
@Getter
public class OperadorEstoqueInvalidoException extends RuntimeException {

    /**
     * O operador de estoque inválido que foi fornecido.
     */
    private final String operadorFornecido;

    /**
     * Constrói a exceção com o operador de estoque inválido.
     *
     * @param operadorFornecido O valor inválido fornecido como operador de estoque.
     */
    public OperadorEstoqueInvalidoException(String operadorFornecido) {
        super(String.format(
                "Operador de estoque inválido: '%s'. Os operadores válidos são: 'GTE' (≥) ou 'LTE' (≤).",
                operadorFornecido
        ));
        this.operadorFornecido = operadorFornecido;
    }
}

