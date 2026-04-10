package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
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
                "As margens aplicadas resultam em uma largura de bloco nula ou negativa (" +
                        HumanNumberDisplayFormatter.formatLengthCm(larguraFinal) +
                        "). A largura dos produtos é " +
                        HumanNumberDisplayFormatter.formatLengthCm(larguraProdutos) +
                        " e as margens somam " +
                        HumanNumberDisplayFormatter.formatLengthCm(somaMargens) +
                        "."
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
                "A soma da largura dos produtos (" +
                        HumanNumberDisplayFormatter.formatLengthCm(larguraProdutos) +
                        ") e das margens (" +
                        HumanNumberDisplayFormatter.formatLengthCm(margemEsquerda) +
                        " + " +
                        HumanNumberDisplayFormatter.formatLengthCm(margemDireita) +
                        ") excede a largura do lote (" +
                        HumanNumberDisplayFormatter.formatLengthCm(larguraLote) +
                        ")."
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
                "As margens aplicadas resultam em um comprimento final nulo ou negativo (" +
                        HumanNumberDisplayFormatter.formatLengthCm(comprimentoFinal) +
                        ")."
        );
    }
}
