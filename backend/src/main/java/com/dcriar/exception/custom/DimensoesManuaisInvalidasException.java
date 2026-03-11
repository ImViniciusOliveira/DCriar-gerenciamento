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
    public DimensoesManuaisInvalidasException(String message) {
        super(message);
    }

    /**
     * Construtor para o erro onde a largura do corte manual é maior que a largura do lote.
     * @param larguraCorteManual A largura informada pelo usuário.
     * @param larguraLote A largura máxima do lote.
     */
    public DimensoesManuaisInvalidasException(BigDecimal larguraCorteManual, BigDecimal larguraLote) {
        super(String.format(
                "A largura do corte manual (%.2f cm) não pode ser maior que a largura do lote (%.2f cm).",
                larguraCorteManual, larguraLote
        ));
    }

    /**
     * Construtor para o erro onde o comprimento final não é suficiente para a quantidade de produtos.
     * @param comprimentoFinal O comprimento informado pelo usuário.
     * @param comprimentoMinimo O comprimento mínimo calculado para a produção.
     */
    public DimensoesManuaisInvalidasException(BigDecimal comprimentoFinal, BigDecimal comprimentoMinimo, String message) {
        super(String.format(
                "O comprimento final (%.2f cm) não é suficiente para produzir a quantidade solicitada. Mínimo necessário: %.2f cm.",
                comprimentoFinal, comprimentoMinimo
        ));
    }
}
