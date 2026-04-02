package com.dcriar.domain.product.entity.enums;

/**
 * Enum que representa os diferentes tipos de movimentação que podem ocorrer no estoque de um produto acabado.
 */
public enum TipoMovimentacaoProduto {

    /**
     * Entrada de produtos acabados no estoque proveniente de uma ordem de produção.
     */
    ENTRADA_PRODUCAO,

    /**
     * Saída de produtos do estoque devido a uma venda.
     */
    SAIDA_VENDA,

    /**
     * Ajuste manual na quantidade de estoque de um produto, podendo ser entrada ou saída.
     */
    AJUSTE_MANUAL,

    /**
     * Representa o estorno de uma venda, devolvendo o produto ao estoque.
     * Utilizado quando uma Venda é excluída ou atualizada. (Quantidade positiva)
     */
    ESTORNO_VENDA,

    /**
     * Representa o estorno de uma entrada de produção, removendo o produto do estoque.
     * Utilizado quando uma Ordem de Produção é excluída. (Quantidade negativa)
     */
    ESTORNO_PRODUCAO
}
