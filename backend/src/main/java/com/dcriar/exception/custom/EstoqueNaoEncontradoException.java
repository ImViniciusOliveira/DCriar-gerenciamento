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
    private final String produtoLabel;

    /**
     * O ID do canal de venda.
     */
    private final Long canalVendaId;
    private final String nomeCanalVenda;

    /**
     * Constrói a exceção com os detalhes do estoque não encontrado.
     *
     * @param produtoId O ID do produto.
     * @param canalVendaId O ID do canal de venda.
     */
    public EstoqueNaoEncontradoException(Long produtoId, String produtoLabel, Long canalVendaId, String nomeCanalVenda) {
        super(String.format(
                "Não foi encontrado estoque para o produto '%s' no canal de venda '%s'. Verifique os dados informados e tente novamente.",
                produtoLabel,
                nomeCanalVenda
        ));
        this.produtoId = produtoId;
        this.produtoLabel = produtoLabel;
        this.canalVendaId = canalVendaId;
        this.nomeCanalVenda = nomeCanalVenda;
    }
}
