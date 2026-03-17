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
    private ProdutoNaoCabeNoLoteException(String message) {
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

    public static ProdutoNaoCabeNoLoteException margensLateraisExcedemLarguraLote() {
        return new ProdutoNaoCabeNoLoteException("A soma das margens laterais é maior ou igual à largura do lote.");
    }

    public static ProdutoNaoCabeNoLoteException produtoNaoCabeEmNenhumaOrientacao() {
        return new ProdutoNaoCabeNoLoteException("O produto não cabe na largura do lote em nenhuma orientação.");
    }

    public static ProdutoNaoCabeNoLoteException comprimentoBlocoExcedeComprimentoLote(
            BigDecimal comprimentoBloco,
            BigDecimal comprimentoLote
    ) {
        return new ProdutoNaoCabeNoLoteException(String.format(
                "O comprimento do bloco de corte (%.2fcm) excede o comprimento do lote (%.2fcm).",
                comprimentoBloco,
                comprimentoLote
        ));
    }

    public static ProdutoNaoCabeNoLoteException larguraBlocoExcedeLarguraLote(
            BigDecimal larguraBloco,
            BigDecimal larguraLote
    ) {
        return new ProdutoNaoCabeNoLoteException(String.format(
                "A largura do bloco de corte (%.2fcm) excede a largura do lote (%.2fcm).",
                larguraBloco,
                larguraLote
        ));
    }
}
