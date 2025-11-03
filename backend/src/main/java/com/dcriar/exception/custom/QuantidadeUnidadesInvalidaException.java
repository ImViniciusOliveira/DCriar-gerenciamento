package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando a quantidade de unidades base para um cálculo (como o de custo)
 * é menor ou igual a zero, o que tornaria a operação inválida.
 */
@Getter
public class QuantidadeUnidadesInvalidaException extends RuntimeException {

    /**
     * Constrói a exceção com uma mensagem de erro padrão.
     */
    public QuantidadeUnidadesInvalidaException() {
        super("A quantidade total de unidades base para cálculo de custo deve ser maior que zero.");
    }

    /**
     * Constrói a exceção com uma mensagem personalizada.
     *
     * @param message A mensagem de erro.
     */
    public QuantidadeUnidadesInvalidaException(String message) {
        super(message);
    }
}
