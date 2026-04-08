package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um Corte Realizado não é encontrado no sistema.
 */
@Getter
public class CorteRealizadoNaoEncontradoException extends RuntimeException {

    /**
     * O ID do corte realizado não encontrado.
     */
    private final Long id;

    /**
     * Constrói a exceção com o ID do corte realizado não encontrado.
     *
     * @param id O ID do corte realizado.
     */
    public CorteRealizadoNaoEncontradoException(Long id) {
        super("Corte realizado #" + id + " não foi encontrado. Verifique o identificador informado e tente novamente.");
        this.id = id;
    }
}
