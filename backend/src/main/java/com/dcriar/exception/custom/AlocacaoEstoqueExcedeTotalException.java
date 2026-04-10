package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import lombok.Getter;

/**
 * Exceção lançada quando uma tentativa de alocar estoque para um canal de venda
 * faria com que o total distribuído excedesse o estoque físico total disponível.
 */
@Getter
public class AlocacaoEstoqueExcedeTotalException extends RuntimeException {

    private final Long produtoId;
    private final String produtoLabel;
    private final Long canalVendaId;
    private final String nomeCanalVenda;
    private final int quantidadeParaAlocar;
    private final int novoTotalDistribuido;
    private final int estoqueFisicoTotal;
    private final int quantidadeMaximaPermitida;

    public AlocacaoEstoqueExcedeTotalException(
            Long produtoId,
            String produtoLabel,
            Long canalVendaId,
            String nomeCanalVenda,
            int quantidadeParaAlocar,
            int novoTotalDistribuido,
            int estoqueFisicoTotal
    ) {
        super(String.format(
                "Não é possível alocar %s %s do produto '%s' para o canal de venda '%s'. O total distribuído ficaria em %s %s, acima do estoque físico total de %s.",
                HumanNumberDisplayFormatter.formatInteger(quantidadeParaAlocar),
                quantidadeParaAlocar == 1 ? "unidade" : "unidades",
                produtoLabel,
                nomeCanalVenda,
                HumanNumberDisplayFormatter.formatInteger(novoTotalDistribuido),
                novoTotalDistribuido == 1 ? "unidade" : "unidades",
                HumanNumberDisplayFormatter.formatInteger(estoqueFisicoTotal)
        ));
        this.produtoId = produtoId;
        this.produtoLabel = produtoLabel;
        this.canalVendaId = canalVendaId;
        this.nomeCanalVenda = nomeCanalVenda;
        this.quantidadeParaAlocar = quantidadeParaAlocar;
        this.novoTotalDistribuido = novoTotalDistribuido;
        this.estoqueFisicoTotal = estoqueFisicoTotal;
        this.quantidadeMaximaPermitida = Math.max(estoqueFisicoTotal - (novoTotalDistribuido - quantidadeParaAlocar), 0);
    }
}
