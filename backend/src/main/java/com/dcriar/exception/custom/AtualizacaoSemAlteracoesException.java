package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class AtualizacaoSemAlteracoesException extends RuntimeException {

    private final String recurso;
    private final Long recursoId;

    private AtualizacaoSemAlteracoesException(String recurso, Long recursoId, String message) {
        super(message);
        this.recurso = recurso;
        this.recursoId = recursoId;
    }

    public static AtualizacaoSemAlteracoesException para(String recurso, Long recursoId, String message) {
        return new AtualizacaoSemAlteracoesException(recurso, recursoId, message);
    }
}
