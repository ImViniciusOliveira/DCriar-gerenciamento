package com.dcriar.domain.stock.model;

import com.dcriar.domain.stock.entity.LoteMateriaPrima;

import java.util.List;

/**
 * Representa um nó da árvore de retalhos derivada de um lote principal ou de uma ordem de produção.
 *
 * @param lote O lote representado neste nó.
 * @param nivel A profundidade do nó na árvore, começando em 1 para filhos diretos.
 * @param loteDeOrigemId O ID do lote pai imediato.
 * @param ordemDeProducaoOrigemId O ID da ordem de produção raiz que originou este retalho, quando houver.
 * @param caminhoIds O caminho completo de IDs desde a raiz até o lote atual.
 * @param possuiMovimentacaoDeSaida Indica se o lote já sofreu alguma movimentação de saída.
 */
public record LoteRetalhoHierarchyItem(
        LoteMateriaPrima lote,
        int nivel,
        Long loteDeOrigemId,
        Long ordemDeProducaoOrigemId,
        List<Long> caminhoIds,
        boolean possuiMovimentacaoDeSaida
) {
}
