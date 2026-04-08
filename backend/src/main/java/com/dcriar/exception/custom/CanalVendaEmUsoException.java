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
    private final Set<String> estoqueLabels;
    private final Set<Long> vendaIds;
    private final Set<String> vendaLabels;
    private final Set<Long> ordemDeProducaoIds;
    private final Set<String> ordemDeProducaoLabels;

    public CanalVendaEmUsoException(
            Long canalVendaId,
            String nomeCanalVenda,
            Set<Long> estoqueIds,
            Set<String> estoqueLabels,
            Set<Long> vendaIds,
            Set<String> vendaLabels,
            Set<Long> ordemDeProducaoIds,
            Set<String> ordemDeProducaoLabels
    ) {
        super(String.format(
                "Não é possível excluir o canal de venda '%s' porque ele ainda está em uso em %s, %s e %s. Remova ou ajuste esses vínculos antes de excluir o canal.",
                nomeCanalVenda,
                descreverQuantidade(estoqueIds.size(), "estoque", "estoques"),
                descreverQuantidade(vendaIds.size(), "venda", "vendas"),
                descreverQuantidade(ordemDeProducaoIds.size(), "ordem de produção", "ordens de produção")
        ));
        this.canalVendaId = canalVendaId;
        this.nomeCanalVenda = nomeCanalVenda;
        this.estoqueIds = estoqueIds;
        this.estoqueLabels = estoqueLabels;
        this.vendaIds = vendaIds;
        this.vendaLabels = vendaLabels;
        this.ordemDeProducaoIds = ordemDeProducaoIds;
        this.ordemDeProducaoLabels = ordemDeProducaoLabels;
    }

    private static String descreverQuantidade(int quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }
}
