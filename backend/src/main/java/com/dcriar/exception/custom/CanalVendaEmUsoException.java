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
                "Canal de venda com id %d está em uso e não pode ser excluído. Estoques: %s, Vendas: %s, Ordens de produção: %s",
                canalVendaId,
                estoqueIds,
                vendaIds,
                ordemDeProducaoIds
        ));
        this.canalVendaId = canalVendaId;
        this.estoqueIds = estoqueIds;
        this.vendaIds = vendaIds;
        this.ordemDeProducaoIds = ordemDeProducaoIds;
    }
}
