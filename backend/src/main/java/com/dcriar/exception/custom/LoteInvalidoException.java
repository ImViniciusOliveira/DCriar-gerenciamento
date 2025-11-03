package com.dcriar.exception.custom;

/**
 * Exceção lançada quando um ou mais IDs de lote de matéria-prima fornecidos em uma operação são inválidos ou não foram encontrados.
 */
public class LoteInvalidoException extends RuntimeException {

    public LoteInvalidoException(String message) {
        super(message);
    }
}
