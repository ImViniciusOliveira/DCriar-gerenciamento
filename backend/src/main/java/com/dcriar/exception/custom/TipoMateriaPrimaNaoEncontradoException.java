package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação tenta acessar um {@link com.dcriar.domain.stock.entity.TipoMateriaPrima}
 * que não existe no banco de dados, usando seu ID.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 404 (Not Found).
 */
@Getter
public class TipoMateriaPrimaNaoEncontradoException extends RuntimeException {

    /**
     * O ID do tipo de matéria-prima que não foi encontrado.
     */
    private final Long materiaPrimaId;

    /**
     * Constrói a exceção com o ID do tipo de matéria-prima não encontrado.
     *
     * @param materiaPrimaId O ID utilizado na busca que falhou.
     */
    public TipoMateriaPrimaNaoEncontradoException(Long materiaPrimaId) {
        super(String.format("Tipo de matéria-prima #%d não foi encontrado. Verifique o identificador informado e tente novamente.", materiaPrimaId));
        this.materiaPrimaId = materiaPrimaId;
    }
}
