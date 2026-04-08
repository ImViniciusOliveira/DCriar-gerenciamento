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
    private final String nomeCanalVenda;
    private final Set<Long> estoqueIds;
    private final Set<Long> vendaIds;
    private final Set<Long> ordemDeProducaoIds;

    public CanalVendaEmUsoException(
            Long canalVendaId,
            String nomeCanalVenda,
            Set<Long> estoqueIds,
            Set<Long> vendaIds,
            Set<Long> ordemDeProducaoIds
    ) {
        super(String.format(
                "Não é possível excluir o canal de venda '%s' porque ele ainda está em uso em %d estoque(s), %d venda(s) e %d ordem(ns) de produção. Remova ou ajuste esses vínculos antes de excluir o canal.",
                nomeCanalVenda,
                estoqueIds.size(),
                vendaIds.size(),
                ordemDeProducaoIds.size()
        ));
        this.canalVendaId = canalVendaId;
        this.nomeCanalVenda = nomeCanalVenda;
        this.estoqueIds = estoqueIds;
        this.vendaIds = vendaIds;
        this.ordemDeProducaoIds = ordemDeProducaoIds;
    }
}
