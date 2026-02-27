package com.dcriar.domain.production.service;

import com.dcriar.domain.production.model.PlanoDeConsumo;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface responsável por realizar os cálculos de consumo de matéria-prima.
 * <p>
 * Define o contrato para calcular como uma quantidade necessária de matéria-prima
 * será distribuída e consumida a partir de uma lista de lotes disponíveis.
 */
public interface ConsumoCalculatorService {

    /**
     * Calcula o plano de consumo detalhado para uma dada necessidade de matéria-prima
     * a partir de uma lista de lotes.
     * <p>
     * O método determina quanto será consumido de cada lote, na ordem fornecida,
     * e qual será o saldo restante em cada um, sem persistir nenhuma alteração.
     *
     * @param lotes A lista de lotes disponíveis para consumo.
     * @param consumoNecessario A quantidade total de matéria-prima necessária.
     * @return um objeto {@link PlanoDeConsumo} contendo o plano detalhado.
     */
    PlanoDeConsumo calcularPlanoDeConsumo(List<LoteMateriaPrima> lotes, BigDecimal consumoNecessario);
}
