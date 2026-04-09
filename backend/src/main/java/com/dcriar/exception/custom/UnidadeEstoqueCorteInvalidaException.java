package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import lombok.Getter;

/**
 * Exceção lançada quando uma operação de corte recebe
 * um lote com unidade de estoque incompatível com o fluxo geométrico.
 */
@Getter
public class UnidadeEstoqueCorteInvalidaException extends RuntimeException {

    private final UnidadeDeMedida unidadeDeEstoque;

    private UnidadeEstoqueCorteInvalidaException(UnidadeDeMedida unidadeDeEstoque, String message) {
        super(message);
        this.unidadeDeEstoque = unidadeDeEstoque;
    }

    public static UnidadeEstoqueCorteInvalidaException unidadeNaoSuportada(UnidadeDeMedida unidadeDeEstoque) {
        return new UnidadeEstoqueCorteInvalidaException(
                unidadeDeEstoque,
                String.format(
                "A unidade de estoque '%s' não é suportada para operações de corte. Use uma unidade geométrica compatível com o lote.",
                unidadeDeEstoque
                )
        );
    }
}
