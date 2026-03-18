package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;

/**
 * Exceção lançada quando uma operação de corte recebe
 * um lote com unidade de estoque incompatível com o fluxo geométrico.
 */
public class UnidadeEstoqueCorteInvalidaException extends RuntimeException {

    private UnidadeEstoqueCorteInvalidaException(String message) {
        super(message);
    }

    public static UnidadeEstoqueCorteInvalidaException unidadeNaoSuportada(UnidadeDeMedida unidadeDeEstoque) {
        return new UnidadeEstoqueCorteInvalidaException(String.format(
                "A unidade de estoque '%s' não é suportada para operações de corte. Use uma unidade geométrica compatível com o lote.",
                unidadeDeEstoque
        ));
    }
}
