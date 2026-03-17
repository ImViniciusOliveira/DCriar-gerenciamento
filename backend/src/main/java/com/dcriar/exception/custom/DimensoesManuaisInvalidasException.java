package com.dcriar.exception.custom;

import java.math.BigDecimal;

/**
 * Exceção lançada quando os parâmetros fornecidos para um corte em modo manual são inválidos ou inconsistentes.
 * Cobre cenários como dimensões que não comportam o produto ou que excedem os limites do lote.
 */
public class DimensoesManuaisInvalidasException extends RuntimeException {

    /**
     * Construtor genérico para mensagens de erro simples.
     * @param message A mensagem de erro.
     */
    private DimensoesManuaisInvalidasException(String message) {
        super(message);
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
        return new DimensoesManuaisInvalidasException(String.format(
                "A largura do corte manual (%.2f cm) não pode ser maior que a largura do lote (%.2f cm).",
                larguraCorteManual,
                larguraLote
        ));
    }

    public static DimensoesManuaisInvalidasException comprimentoMaiorQueLote(
            BigDecimal comprimentoCorteManual,
            BigDecimal comprimentoLote
    ) {
        return new DimensoesManuaisInvalidasException(String.format(
                "O comprimento do corte manual (%.2f cm) não pode ser maior que o comprimento do lote (%.2f cm).",
                comprimentoCorteManual,
                comprimentoLote
        ));
    }

}
