package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma operação de produção (corte ou consumo direto)
 * é tentada para um produto que não é compatível com esse tipo de produção,
 * ou quando o lote de matéria-prima selecionado é incompatível com o produto.
 */
@Getter
public class TipoProducaoIncompativelException extends RuntimeException {

    private final String nomeProduto;
    private final String nomeMateriaPrimaProduto;
    private final Long loteId;
    private final String nomeMateriaPrimaLote;

    /**
     * Construtor para incompatibilidade geral de tipo de produção.
     * @param message A mensagem de erro.
     */
    public TipoProducaoIncompativelException(String message) {
        super(message);
        this.nomeProduto = null;
        this.nomeMateriaPrimaProduto = null;
        this.loteId = null;
        this.nomeMateriaPrimaLote = null;
    }

    /**
     * Construtor específico para incompatibilidade entre lote e produto.
     */
    public TipoProducaoIncompativelException(String nomeProduto, String nomeMateriaPrimaProduto, Long loteId, String nomeMateriaPrimaLote) {
        super(String.format("O lote #%d (%s) não é compatível com a matéria-prima do produto '%s' (%s).",
                loteId, nomeMateriaPrimaLote, nomeProduto, nomeMateriaPrimaProduto));
        this.nomeProduto = nomeProduto;
        this.nomeMateriaPrimaProduto = nomeMateriaPrimaProduto;
        this.loteId = loteId;
        this.nomeMateriaPrimaLote = nomeMateriaPrimaLote;
    }
}
