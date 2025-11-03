package com.dcriar.exception.custom;

import lombok.Getter;

import java.util.Set;

/**
 * Exceção lançada quando uma operação tenta acessar um ou mais {@link com.dcriar.domain.stock.entity.LoteMateriaPrima}
 * que não existem no banco de dados, usando seus IDs.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 404 (Not Found).
 */
@Getter
public class LotesMateriaPrimaNaoEncontradosException extends RuntimeException {

    /**
     * O conjunto de IDs dos lotes de matéria-prima que não foram encontrados.
     */
    private final Set<Long> ids;

    /**
     * Constrói a exceção com o conjunto de IDs dos lotes não encontrados.
     *
     * @param ids O conjunto de IDs utilizados na busca que falhou.
     */
    public LotesMateriaPrimaNaoEncontradosException(Set<Long> ids) {
        super(String.format("Lotes de matéria-prima não encontrados com os IDs: %s", ids));
        this.ids = ids;
    }
}
