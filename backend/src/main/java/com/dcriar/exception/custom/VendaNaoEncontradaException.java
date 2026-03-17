package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação tenta acessar uma Venda (Sale)
 * que não existe no banco de dados, usando seu ID.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 404 (Not Found).
 */
@Getter
public class VendaNaoEncontradaException extends RuntimeException {

    /**
     * O ID da venda que não foi encontrada.
     */
    private final Long vendaId;

    /**
     * Constrói a exceção com o ID da venda não encontrada.
     *
     * @param vendaId O ID utilizado na busca que falhou.
     */
    public VendaNaoEncontradaException(Long vendaId) {
        super(String.format("Venda não encontrada com o ID: %d", vendaId));
        this.vendaId = vendaId;
    }
}
