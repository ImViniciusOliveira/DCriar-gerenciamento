package com.dcriar.exception.custom;

/**
 * Exceção lançada quando uma operação de produção (corte ou consumo direto)
 * é tentada para um produto que não é compatível com esse tipo de produção.
 */
public class TipoProducaoIncompativelException extends RuntimeException {

    public TipoProducaoIncompativelException(String message) {
        super(message);
    }
}
