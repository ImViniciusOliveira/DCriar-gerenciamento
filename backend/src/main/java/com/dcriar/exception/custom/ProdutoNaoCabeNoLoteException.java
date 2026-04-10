package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
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
        super("A largura do corte manual (" + HumanNumberDisplayFormatter.formatLengthCm(larguraCorteManual) +
                ") não comporta a largura do produto (" + HumanNumberDisplayFormatter.formatLengthCm(larguraProduto) + ").");
    }

    /**
     * Construtor para o erro onde o produto com margens não cabe no lote em nenhuma orientação.
     * @param larguraProdutoComMargens Largura do produto + margens.
     * @param comprimentoProdutoComMargens Comprimento do produto + margens (para orientação rotacionada).
     * @param larguraLote A largura do lote.
     */
    public ProdutoNaoCabeNoLoteException(BigDecimal larguraProdutoComMargens, BigDecimal comprimentoProdutoComMargens, BigDecimal larguraLote) {
        super("Produto com margens não cabe no lote. Largura necessária (" +
                HumanNumberDisplayFormatter.formatLengthCm(larguraProdutoComMargens) +
                ") e Comprimento necessário (" +
                HumanNumberDisplayFormatter.formatLengthCm(comprimentoProdutoComMargens) +
                ") excedem a largura do lote (" +
                HumanNumberDisplayFormatter.formatLengthCm(larguraLote) +
                ").");
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
                "O comprimento do bloco de corte (%s) excede o comprimento do lote (%s).",
                HumanNumberDisplayFormatter.formatLengthCm(comprimentoBloco),
                HumanNumberDisplayFormatter.formatLengthCm(comprimentoLote)
        ));
    }

    public static ProdutoNaoCabeNoLoteException larguraBlocoExcedeLarguraLote(
            BigDecimal larguraBloco,
            BigDecimal larguraLote
    ) {
        return new ProdutoNaoCabeNoLoteException(String.format(
                "A largura do bloco de corte (%s) excede a largura do lote (%s).",
                HumanNumberDisplayFormatter.formatLengthCm(larguraBloco),
                HumanNumberDisplayFormatter.formatLengthCm(larguraLote)
        ));
    }
}
