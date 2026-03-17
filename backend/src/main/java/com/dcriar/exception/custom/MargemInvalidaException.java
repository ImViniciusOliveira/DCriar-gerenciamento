package com.dcriar.exception.custom;

import java.math.BigDecimal;

/**
 * Exceção lançada quando as margens especificadas para um corte são inválidas
 * (ex: a soma das margens excede a dimensão do material).
 */
public class MargemInvalidaException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param message A mensagem explicando a invalidez da margem.
     */
    private MargemInvalidaException(String message) {
        super(message);
    }

    public static MargemInvalidaException larguraFinalNaoPositiva(
            BigDecimal larguraFinal,
            BigDecimal larguraProdutos,
            BigDecimal somaMargens
    ) {
        return new MargemInvalidaException(String.format(
                "As margens aplicadas resultam em uma largura de bloco nula ou negativa (%.2fcm). A largura dos produtos é %.2fcm e as margens somam %.2fcm.",
                larguraFinal,
                larguraProdutos,
                somaMargens
        ));
    }

    public static MargemInvalidaException larguraComMargensExcedeLote(
            BigDecimal larguraProdutos,
            BigDecimal margemEsquerda,
            BigDecimal margemDireita,
            BigDecimal larguraLote
    ) {
        return new MargemInvalidaException(String.format(
                "A soma da largura dos produtos (%.2fcm) e das margens (%.2fcm + %.2fcm) excede a largura do lote (%.2fcm).",
                larguraProdutos,
                margemEsquerda,
                margemDireita,
                larguraLote
        ));
    }

    public static MargemInvalidaException comprimentoFinalNaoPositivo(BigDecimal comprimentoFinal) {
        return new MargemInvalidaException(String.format(
                "As margens aplicadas resultam em um comprimento final nulo ou negativo (%.2fcm).",
                comprimentoFinal
        ));
    }
}
