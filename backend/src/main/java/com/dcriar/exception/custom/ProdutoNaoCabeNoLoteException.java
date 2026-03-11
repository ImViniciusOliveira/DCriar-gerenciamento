package com.dcriar.exception.custom;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exceção lançada quando um produto não pode ser cortado de um lote de matéria-prima
 * porque suas dimensões, possivelmente combinadas com margens, excedem as dimensões úteis do lote.
 */
@Getter
public class ProdutoNaoCabeNoLoteException extends RuntimeException {

    /**
     * Construtor genérico para mensagens de erro simples.
     * @param message A mensagem explicando por que o produto não cabe no lote.
     */
    public ProdutoNaoCabeNoLoteException(String message) {
        super(message);
    }

    /**
     * Construtor para o erro onde a largura do corte manual não comporta a largura do produto.
     * @param larguraCorteManual A largura do corte informada.
     * @param larguraProduto A largura do produto.
     */
    public ProdutoNaoCabeNoLoteException(BigDecimal larguraCorteManual, BigDecimal larguraProduto) {
        super(String.format(
                "A largura do corte manual (%.2f cm) não comporta a largura do produto (%.2f cm).",
                larguraCorteManual, larguraProduto
        ));
    }

    /**
     * Construtor para o erro onde o produto com margens não cabe no lote em nenhuma orientação.
     * @param larguraProdutoComMargens Largura do produto + margens.
     * @param comprimentoProdutoComMargens Comprimento do produto + margens (para orientação rotacionada).
     * @param larguraLote A largura do lote.
     */
    public ProdutoNaoCabeNoLoteException(BigDecimal larguraProdutoComMargens, BigDecimal comprimentoProdutoComMargens, BigDecimal larguraLote) {
        super(String.format(
                "Produto com margens não cabe no lote. Largura necessária (%.2f cm) e Comprimento necessário (%.2f cm) excedem a largura do lote (%.2f cm).",
                larguraProdutoComMargens, comprimentoProdutoComMargens, larguraLote
        ));
    }
}
