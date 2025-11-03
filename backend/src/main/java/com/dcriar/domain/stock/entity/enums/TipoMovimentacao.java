package com.dcriar.domain.stock.entity.enums;

/**
 * Enum que representa os diferentes tipos de movimentação que podem ocorrer no estoque de um lote de matéria-prima.
 * <p>
 * A utilização de um Enum garante a consistência e a segurança dos tipos de dados,
 * servindo como base para o "Livro-Razão" do sistema de estoque.
 */
public enum TipoMovimentacao {

    /**
     * Representa a entrada de um novo lote no estoque, geralmente por meio de uma compra.
     * Esta movimentação sempre terá uma quantidade positiva.
     */
    ENTRADA_COMPRA,

    /**
     * Representa a saída de material do estoque para ser utilizado na fabricação de um produto.
     * Esta movimentação sempre terá uma quantidade negativa.
     */
    SAIDA_PRODUCAO,

    /**
     * Representa uma perda de material que foi descartado por qualquer motivo (dano, vencimento, etc.).
     * Esta movimentação sempre terá uma quantidade negativa.
     */
    PERDA_DESCARTE,

    /**
     * Representa um ajuste manual no estoque, geralmente após uma contagem de inventário físico.
     * A quantidade pode ser positiva (se havia mais material do que o registrado) ou
     * negativa (se havia menos).
     */
    AJUSTE_INVENTARIO,

    /**
     * Regista a entrada de uma sobra (retalho) gerada a partir de um processo de produção. (Quantidade positiva)
     */
    ENTRADA_SOBRA
}
