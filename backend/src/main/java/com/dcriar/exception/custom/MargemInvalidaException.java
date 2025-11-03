package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando as margens especificadas para um corte são inválidas
 * (ex: a soma das margens excede a dimensão do material).
 */
@Getter
public class MargemInvalidaException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param message A mensagem explicando a invalidez da margem.
     */
    public MargemInvalidaException(String message) {
        super(message);
    }
}
