package com.dcriar.exception.custom;

/**
 * Exceção lançada quando uma ordem de produção por corte é criada sem a especificação do lote principal de matéria-prima.
 */
public class LotePrincipalNaoEspecificadoException extends RuntimeException {

    public LotePrincipalNaoEspecificadoException(String message) {
        super(message);
    }
}
