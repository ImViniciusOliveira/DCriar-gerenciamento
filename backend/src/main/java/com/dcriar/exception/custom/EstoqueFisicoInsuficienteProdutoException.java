package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando um ajuste físico tentaria reduzir o estoque de um produto abaixo de zero.
 */
@Getter
public class EstoqueFisicoInsuficienteProdutoException extends RuntimeException {

    private final Long produtoId;
    private final String produtoLabel;
    private final Integer quantidadeRequisitada;
    private final Integer estoqueAtual;
    private final Integer quantidadeMaximaPermitida;

    public EstoqueFisicoInsuficienteProdutoException(
            Long produtoId,
            String produtoLabel,
            int quantidadeRequisitada,
            int estoqueAtual
    ) {
        super(String.format(
                "Nao e possivel remover %d %s do estoque fisico do produto '%s' porque ha apenas %d %s disponiveis.",
                Math.abs(quantidadeRequisitada),
                descreverUnidade(Math.abs(quantidadeRequisitada)),
                produtoLabel,
                estoqueAtual,
                descreverUnidade(estoqueAtual)
        ));
        this.produtoId = produtoId;
        this.produtoLabel = produtoLabel;
        this.quantidadeRequisitada = quantidadeRequisitada;
        this.estoqueAtual = estoqueAtual;
        this.quantidadeMaximaPermitida = Math.max(estoqueAtual, 0);
    }

    private static String descreverUnidade(int quantidade) {
        return quantidade == 1 ? "unidade" : "unidades";
    }
}
