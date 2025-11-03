package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um atributo esperado de um lote de matéria-prima
 * é inválido, ausente ou tem um tipo de dado incorreto.
 */
@Getter
public class AtributoLoteInvalidoException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param message A mensagem explicando o problema com o atributo do lote.
     */
    public AtributoLoteInvalidoException(String message) {
        super(message);
    }
}
