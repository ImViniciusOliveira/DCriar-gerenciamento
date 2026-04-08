package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Set;

/**
 * Exceção lançada ao tentar excluir um {@link com.dcriar.domain.stock.entity.TipoMateriaPrima}
 * que ainda está em uso por um ou mais {@link com.dcriar.domain.stock.entity.LoteMateriaPrima}.
 * <p>
 * Esta exceção carrega os IDs dos lotes que referenciam o tipo de matéria-prima,
 * permitindo que o handler de exceções retorne uma mensagem de erro detalhada.
 */
@Getter
public class TipoMateriaPrimaEmUsoException extends RuntimeException {

    /**
     * O ID do tipo de matéria-prima que não pôde ser excluído.
     */
    private final Long tipoMateriaPrimaId;
    private final String nomeTipoMateriaPrima;

    /**
     * Um conjunto de IDs dos lotes que estão utilizando o tipo de matéria-prima.
     */
    private final Set<Long> loteIds;
    private final Set<String> loteLabels;

    /**
     * Constrói a exceção com os detalhes da violação.
     *
     * @param tipoMateriaPrimaId O ID do tipo de matéria-prima que se tentou excluir.
     * @param loteIds O conjunto de IDs dos lotes que impedem a exclusão.
     */
    public TipoMateriaPrimaEmUsoException(Long tipoMateriaPrimaId, String nomeTipoMateriaPrima, Set<Long> loteIds, Set<String> loteLabels) {
        super(String.format(
                "Não é possível excluir o tipo de matéria-prima '%s' porque ele ainda está em uso em %s. Remova ou ajuste esses vínculos antes de tentar excluir o cadastro.",
                nomeTipoMateriaPrima,
                descreverQuantidade(loteIds.size(), "lote", "lotes")
        ));
        this.tipoMateriaPrimaId = tipoMateriaPrimaId;
        this.nomeTipoMateriaPrima = nomeTipoMateriaPrima;
        this.loteIds = loteIds;
        this.loteLabels = loteLabels;
    }

    private static String descreverQuantidade(int quantidade, String singular, String plural) {
        return quantidade + " " + (quantidade == 1 ? singular : plural);
    }
}
