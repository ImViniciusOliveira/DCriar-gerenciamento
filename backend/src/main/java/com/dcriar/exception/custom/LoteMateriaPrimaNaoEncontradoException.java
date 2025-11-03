package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação tenta acessar um {@link com.dcriar.domain.stock.entity.LoteMateriaPrima}
 * que não existe no banco de dados, usando seu ID.
 * <p>
 * É tipicamente capturada por um handler global que retorna uma resposta
 * HTTP 404 (Not Found).
 */
@Getter
public class LoteMateriaPrimaNaoEncontradoException extends RuntimeException {

    /**
     * O ID do lote de matéria-prima que não foi encontrado.
     */
    private final Long id;

    /**
     * Constrói a exceção com o ID do lote não encontrado.
     *
     * @param id O ID utilizado na busca que falhou.
     */
    public LoteMateriaPrimaNaoEncontradoException(Long id) {
        super(String.format("Lote de matéria-prima não encontrado com o ID: %d", id));
        this.id = id;
    }
}
