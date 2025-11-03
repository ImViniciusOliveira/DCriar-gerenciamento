package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma tentativa de alocar estoque para um canal de venda
 * faria com que o total distribuído excedesse o estoque físico total disponível.
 */
@Getter
public class AlocacaoEstoqueExcedeTotalException extends RuntimeException {

    private final int quantidadeParaAlocar;
    private final int novoTotalDistribuido;
    private final int estoqueFisicoTotal;

    public AlocacaoEstoqueExcedeTotalException(int quantidadeParaAlocar, int novoTotalDistribuido, int estoqueFisicoTotal) {
        super(String.format(
                "Não é possível alocar %d unidades. O total distribuído (%d) excederia o estoque físico total (%d).",
                quantidadeParaAlocar, novoTotalDistribuido, estoqueFisicoTotal
        ));
        this.quantidadeParaAlocar = quantidadeParaAlocar;
        this.novoTotalDistribuido = novoTotalDistribuido;
        this.estoqueFisicoTotal = estoqueFisicoTotal;
    }
}
