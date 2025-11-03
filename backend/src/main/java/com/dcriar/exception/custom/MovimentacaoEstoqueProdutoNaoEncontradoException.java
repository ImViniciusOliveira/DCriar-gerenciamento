package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma Movimentação de Estoque de Produto não é encontrada no sistema.
 */
@Getter
public class MovimentacaoEstoqueProdutoNaoEncontradoException extends RuntimeException {

    /**
     * O ID da movimentação não encontrada.
     */
    private final Long id;

    /**
     * Constrói a exceção com o ID da movimentação não encontrada.
     *
     * @param id O ID da movimentação.
     */
    public MovimentacaoEstoqueProdutoNaoEncontradoException(Long id) {
        super("Movimentação de Estoque de Produto não encontrada com ID: " + id);
        this.id = id;
    }
}
