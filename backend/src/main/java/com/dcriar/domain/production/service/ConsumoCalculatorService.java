package com.dcriar.domain.production.service;

import com.dcriar.domain.production.model.PlanoDeConsumo;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;

import java.math.BigDecimal;

/**
 * Interface responsável por realizar os cálculos de consumo de matéria-prima.
 * <p>
 * Define o contrato para calcular como uma quantidade necessária de matéria-prima
 * será consumida a partir de um único lote disponível.
 */
public interface ConsumoCalculatorService {

    /**
     * Calcula o plano de consumo detalhado para uma dada necessidade de matéria-prima
     * a partir de um único lote.
     * <p>
     * O método determina quanto será consumido do lote e qual será o saldo restante,
     * sem persistir nenhuma alteração.
     *
     * @param lote O lote disponível para consumo.
     * @param consumoNecessario A quantidade total de matéria-prima necessária.
     * @return um objeto {@link PlanoDeConsumo} contendo o plano detalhado.
     */
    PlanoDeConsumo calcularPlanoDeConsumo(LoteMateriaPrima lote, BigDecimal consumoNecessario);
}
