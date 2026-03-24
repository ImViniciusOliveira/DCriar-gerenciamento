package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando os parâmetros operacionais de um ajuste de lote são inválidos.
 */
@Getter
public class AjusteLoteInvalidoException extends RuntimeException {

    private final String detalhe;

    private AjusteLoteInvalidoException(String detalhe, String message) {
        super(message);
        this.detalhe = detalhe;
    }

    public static AjusteLoteInvalidoException direcaoObrigatoriaParaAjuste() {
        return new AjusteLoteInvalidoException(
                "direcao",
                "A direção do ajuste é obrigatória para a operação de ajuste."
        );
    }

    public static AjusteLoteInvalidoException perdaNaoAceitaDirecao() {
        return new AjusteLoteInvalidoException(
                "direcao",
                "Perda/Descarte não aceita direção manual. A operação sempre reduz o saldo."
        );
    }
}
