package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada ao tentar criar um {@link com.dcriar.domain.stock.entity.TipoMateriaPrima}
 * com um nome que já existe no banco de dados.
 * <p>
 * Esta é uma exceção de conflito, tipicamente capturada por um handler global
 * que retorna uma resposta HTTP 409 (Conflict).
 */
@Getter
public class TipoMateriaPrimaJaExisteException extends RuntimeException {

    /**
     * O nome duplicado que causou a falha.
     */
    private final String nome;

    /**
     * Constrói a exceção com o nome duplicado.
     *
     * @param nome O nome do tipo de matéria-prima que já existe.
     */
    public TipoMateriaPrimaJaExisteException(String nome) {
        super(String.format("Tipo de matéria-prima já existente: %s", nome));
        this.nome = nome;
    }
}
