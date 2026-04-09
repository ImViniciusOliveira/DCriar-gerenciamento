package com.dcriar.exception.custom;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exceção lançada quando as margens especificadas para um corte são inválidas
 * (ex: a soma das margens excede a dimensão do material).
 */
@Getter
public class MargemInvalidaException extends RuntimeException {

    private final String codigo;
    private final BigDecimal larguraFinal;
    private final BigDecimal larguraProdutos;
    private final BigDecimal somaMargens;
    private final BigDecimal margemEsquerda;
    private final BigDecimal margemDireita;
    private final BigDecimal larguraLote;
    private final BigDecimal comprimentoFinal;

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param message A mensagem explicando a invalidez da margem.
     */
    private MargemInvalidaException(
            String codigo,
            BigDecimal larguraFinal,
            BigDecimal larguraProdutos,
            BigDecimal somaMargens,
            BigDecimal margemEsquerda,
            BigDecimal margemDireita,
            BigDecimal larguraLote,
            BigDecimal comprimentoFinal,
            String message
    ) {
        super(message);
        this.codigo = codigo;
        this.larguraFinal = larguraFinal;
        this.larguraProdutos = larguraProdutos;
        this.somaMargens = somaMargens;
        this.margemEsquerda = margemEsquerda;
        this.margemDireita = margemDireita;
        this.larguraLote = larguraLote;
        this.comprimentoFinal = comprimentoFinal;
    }

    public static MargemInvalidaException larguraFinalNaoPositiva(
            BigDecimal larguraFinal,
            BigDecimal larguraProdutos,
            BigDecimal somaMargens
    ) {
        return new MargemInvalidaException(
                "LARGURA_FINAL_NAO_POSITIVA",
                larguraFinal,
                larguraProdutos,
                somaMargens,
                null,
                null,
                null,
                null,
                String.format(
                "As margens aplicadas resultam em uma largura de bloco nula ou negativa (%.2fcm). A largura dos produtos é %.2fcm e as margens somam %.2fcm.",
                larguraFinal,
                larguraProdutos,
                somaMargens
                )
        );
    }

    public static MargemInvalidaException larguraComMargensExcedeLote(
            BigDecimal larguraProdutos,
            BigDecimal margemEsquerda,
            BigDecimal margemDireita,
            BigDecimal larguraLote
    ) {
        return new MargemInvalidaException(
                "LARGURA_COM_MARGENS_EXCEDE_LOTE",
                null,
                larguraProdutos,
                null,
                margemEsquerda,
                margemDireita,
                larguraLote,
                null,
                String.format(
                "A soma da largura dos produtos (%.2fcm) e das margens (%.2fcm + %.2fcm) excede a largura do lote (%.2fcm).",
                larguraProdutos,
                margemEsquerda,
                margemDireita,
                larguraLote
                )
        );
    }

    public static MargemInvalidaException comprimentoFinalNaoPositivo(BigDecimal comprimentoFinal) {
        return new MargemInvalidaException(
                "COMPRIMENTO_FINAL_NAO_POSITIVO",
                null,
                null,
                null,
                null,
                null,
                null,
                comprimentoFinal,
                String.format(
                "As margens aplicadas resultam em um comprimento final nulo ou negativo (%.2fcm).",
                comprimentoFinal
                )
        );
    }
}
