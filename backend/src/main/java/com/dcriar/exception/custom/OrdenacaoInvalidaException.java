package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class OrdenacaoInvalidaException extends RuntimeException {

    private final String recurso;
    private final String campoOrdenacao;
    private final String camposAceitos;

    public OrdenacaoInvalidaException(String recurso, String campoOrdenacao, String camposAceitos) {
        super(String.format(
                "O campo de ordenação '%s' não é suportado para '%s'.",
                campoOrdenacao,
                recurso
        ));
        this.recurso = recurso;
        this.campoOrdenacao = campoOrdenacao;
        this.camposAceitos = camposAceitos;
    }
}
