package com.dcriar.exception.custom;

/**
 * Exceção lançada quando uma ordem de produção por corte é criada sem a especificação do lote principal de matéria-prima.
 */
public class LotePrincipalNaoEspecificadoException extends RuntimeException {

    private LotePrincipalNaoEspecificadoException(String message) {
        super(message);
    }

    public static LotePrincipalNaoEspecificadoException paraProducaoPorCorte() {
        return new LotePrincipalNaoEspecificadoException(
                "A produção por corte exige a especificação de um 'loteId'."
        );
    }
}
