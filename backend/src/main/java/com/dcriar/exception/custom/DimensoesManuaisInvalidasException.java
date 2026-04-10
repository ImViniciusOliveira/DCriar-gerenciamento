package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exceção lançada quando os parâmetros fornecidos para um corte em modo manual são inválidos ou inconsistentes.
 * Cobre cenários como dimensões que não comportam o produto ou que excedem os limites do lote.
 */
@Getter
public class DimensoesManuaisInvalidasException extends RuntimeException {

    private final String codigo;
    private final BigDecimal larguraCorteManual;
    private final BigDecimal larguraLote;
    private final BigDecimal comprimentoCorteManual;
    private final BigDecimal comprimentoLote;

    /**
     * Construtor genérico para mensagens de erro simples.
     * @param message A mensagem de erro.
     */
    private DimensoesManuaisInvalidasException(
            String codigo,
            BigDecimal larguraCorteManual,
            BigDecimal larguraLote,
            BigDecimal comprimentoCorteManual,
            BigDecimal comprimentoLote,
            String message
    ) {
        super(message);
        this.codigo = codigo;
        this.larguraCorteManual = larguraCorteManual;
        this.larguraLote = larguraLote;
        this.comprimentoCorteManual = comprimentoCorteManual;
        this.comprimentoLote = comprimentoLote;
    }

    /**
     * Construtor para o erro onde a largura do corte manual é maior que a largura do lote.
     * @param larguraCorteManual A largura informada pelo usuário.
     * @param larguraLote A largura máxima do lote.
     */
    public static DimensoesManuaisInvalidasException larguraMaiorQueLote(
            BigDecimal larguraCorteManual,
            BigDecimal larguraLote
    ) {
        return new DimensoesManuaisInvalidasException(
                "LARGURA_CORTE_MAIOR_QUE_LOTE",
                larguraCorteManual,
                larguraLote,
                null,
                null,
                "A largura do corte manual (" +
                        HumanNumberDisplayFormatter.formatLengthCm(larguraCorteManual) +
                        ") não pode ser maior que a largura do lote (" +
                        HumanNumberDisplayFormatter.formatLengthCm(larguraLote) +
                        ")."
        );
    }

    public static DimensoesManuaisInvalidasException comprimentoMaiorQueLote(
            BigDecimal comprimentoCorteManual,
            BigDecimal comprimentoLote
    ) {
        return new DimensoesManuaisInvalidasException(
                "COMPRIMENTO_CORTE_MAIOR_QUE_LOTE",
                null,
                null,
                comprimentoCorteManual,
                comprimentoLote,
                "O comprimento do corte manual (" +
                        HumanNumberDisplayFormatter.formatLengthCm(comprimentoCorteManual) +
                        ") não pode ser maior que o comprimento do lote (" +
                        HumanNumberDisplayFormatter.formatLengthCm(comprimentoLote) +
                        ")."
        );
    }

}
