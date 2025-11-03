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

    /**
     * Um conjunto de IDs dos lotes que estão utilizando o tipo de matéria-prima.
     */
    private final Set<Long> loteIds;

    /**
     * Constrói a exceção com os detalhes da violação.
     *
     * @param tipoMateriaPrimaId O ID do tipo de matéria-prima que se tentou excluir.
     * @param loteIds O conjunto de IDs dos lotes que impedem a exclusão.
     */
    public TipoMateriaPrimaEmUsoException(Long tipoMateriaPrimaId, Set<Long> loteIds) {
        super(String.format("O tipo de matéria-prima com id %d está em uso nos lotes: %s e não pode ser excluído.", tipoMateriaPrimaId, loteIds));
        this.tipoMateriaPrimaId = tipoMateriaPrimaId;
        this.loteIds = loteIds;
    }
}
