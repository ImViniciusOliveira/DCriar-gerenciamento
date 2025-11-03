package com.dcriar.domain.product.entity.enums;

/**
 * Enum que representa os tipos de eventos que alteram o estoque físico total
 * de um produto acabado.
 */
public enum TipoMovimentacaoProduto {

    /**
     * Regista a entrada de novos produtos fabricados a partir de uma ordem de produção.
     * (Quantidade positiva)
     */
    ENTRADA_PRODUCAO,

    /**
     * Regista a saída de produtos devido a uma venda. (Ainda a ser implementado)
     * (Quantidade negativa)
     */
    SAIDA_VENDA,

    /**
     * Regista uma correção manual de estoque após uma contagem física ou para
     * justificar entradas/saídas que não vêm da produção ou de vendas (ex: devoluções).
     * (Pode ser positivo ou negativo)
     */
    AJUSTE_MANUAL
}
