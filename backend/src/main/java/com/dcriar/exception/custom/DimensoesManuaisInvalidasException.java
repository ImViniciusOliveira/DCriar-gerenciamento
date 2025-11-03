package com.dcriar.exception.custom;

/**
 * Exceção lançada quando uma ordem de produção em modo manual é criada sem as dimensões finais (largura e comprimento).
 */
public class DimensoesManuaisInvalidasException extends RuntimeException {

    public DimensoesManuaisInvalidasException(String message) {
        super(message);
    }
}
