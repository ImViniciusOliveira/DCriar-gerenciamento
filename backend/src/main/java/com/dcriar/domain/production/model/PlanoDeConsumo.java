package com.dcriar.domain.production.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Encapsula o resultado completo de um cálculo de consumo de matéria-prima.
 * <p>
 * Este record serve como um objeto de transferência de dados (DTO) interno do domínio,
 * fornecendo um plano detalhado de quais lotes serão consumidos, em que quantidade,
 * e qual será o saldo restante projetado para cada um.
 *
 * @param itens A lista de instruções de consumo, detalhando o débito por lote.
 * @param saldosRestantes Um mapa onde a chave é o ID do lote e o valor é o saldo que restaria no lote após o consumo.
 */
public record PlanoDeConsumo(
    List<PlanoDeConsumoItem> itens,
    Map<Long, BigDecimal> saldosRestantes
) {
}
