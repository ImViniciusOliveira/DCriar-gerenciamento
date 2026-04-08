package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Set;

/**
 * Exceção lançada ao tentar excluir um canal de venda que ainda está em uso
 * por estoques, vendas ou ordens de produção.
 */
@Getter
public class CanalVendaEmUsoException extends RuntimeException {

    private final Long canalVendaId;
    private final Set<Long> estoqueIds;
    private final Set<Long> vendaIds;
    private final Set<Long> ordemDeProducaoIds;

    public CanalVendaEmUsoException(
            Long canalVendaId,
            Set<Long> estoqueIds,
            Set<Long> vendaIds,
            Set<Long> ordemDeProducaoIds
    ) {
        super(String.format(
                "Não é possível excluir o canal de venda #%d porque ele ainda está em uso. Estoques vinculados: %s. Vendas vinculadas: %s. Ordens de produção vinculadas: %s. Remova ou ajuste esses vínculos antes de excluir o canal.",
                canalVendaId,
                formatarIds(estoqueIds),
                formatarIds(vendaIds),
                formatarIds(ordemDeProducaoIds)
        ));
        this.canalVendaId = canalVendaId;
        this.estoqueIds = estoqueIds;
        this.vendaIds = vendaIds;
        this.ordemDeProducaoIds = ordemDeProducaoIds;
    }

    private static String formatarIds(Set<Long> ids) {
        return ids == null || ids.isEmpty() ? "nenhum" : ids.toString();
    }
}
