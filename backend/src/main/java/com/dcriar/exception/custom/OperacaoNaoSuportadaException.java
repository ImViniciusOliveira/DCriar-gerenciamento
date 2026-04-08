package com.dcriar.exception.custom;

import lombok.Getter;

@Getter
public class OperacaoNaoSuportadaException extends RuntimeException {

    private final String recurso;
    private final String operacao;
    private final String alternativaSugerida;

    private OperacaoNaoSuportadaException(
            String message,
            String recurso,
            String operacao,
            String alternativaSugerida
    ) {
        super(message);
        this.recurso = recurso;
        this.operacao = operacao;
        this.alternativaSugerida = alternativaSugerida;
    }

    public static OperacaoNaoSuportadaException putProdutoUsePatch() {
        return new OperacaoNaoSuportadaException(
                "A atualização completa (PUT) de produtos não é suportada. Utilize o PATCH.",
                "produto",
                "PUT",
                "PATCH /api/v1/produtos/{id}"
        );
    }
}
