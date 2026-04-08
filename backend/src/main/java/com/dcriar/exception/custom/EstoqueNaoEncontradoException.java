package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um registro de Estoque para uma combinação específica
 * de produto e canal de venda não é encontrado.
 */
@Getter
public class EstoqueNaoEncontradoException extends RuntimeException {

    /**
     * O ID do produto.
     */
    private final Long produtoId;

    /**
     * O ID do canal de venda.
     */
    private final Long canalVendaId;

    /**
     * Constrói a exceção com os detalhes do estoque não encontrado.
     *
     * @param produtoId O ID do produto.
     * @param canalVendaId O ID do canal de venda.
     */
    public EstoqueNaoEncontradoException(Long produtoId, Long canalVendaId) {
        super(String.format(
                "Não foi encontrado estoque para o produto #%d no canal de venda #%d. Verifique os identificadores informados e tente novamente.",
                produtoId,
                canalVendaId
        ));
        this.produtoId = produtoId;
        this.canalVendaId = canalVendaId;
    }
}
