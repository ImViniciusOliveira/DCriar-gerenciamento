package com.dcriar.domain.production.model;

import com.dcriar.domain.stock.entity.LoteMateriaPrima;

import java.math.BigDecimal;

/**
 * Representa uma única instrução de consumo dentro de um plano de consumo.
 * <p>
 * Este record é um objeto de transferência de dados (DTO) interno do domínio,
 * contendo o lote a ser consumido e a quantidade exata a ser debitada dele.
 *
 * @param lote O lote de matéria-prima do qual o material será consumido.
 * @param quantidadeAConsumir A quantidade exata a ser consumida deste lote.
 */
public record PlanoDeConsumoItem(
    LoteMateriaPrima lote,
    BigDecimal quantidadeAConsumir
) {
}
