package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import lombok.Getter;

/**
 * Exceção lançada quando se tenta calcular o custo de um lote de matéria-prima
 * com uma combinação de unidade de estoque e unidade de consumo incompatível.
 */
@Getter
public class CalculoCustoIncompativelException extends RuntimeException {

    /**
     * Constrói a exceção com a mensagem de erro.
     *
     * @param unidadeEstoque A unidade de estoque do lote.
     * @param unidadeConsumo A unidade de consumo esperada.
     */
    public CalculoCustoIncompativelException(UnidadeDeMedida unidadeEstoque, UnidadeDeMedida unidadeConsumo) {
        super(String.format(
                "Cálculo de custo para %s só é suportado com consumo em %s.",
                unidadeEstoque.getDescricao(), unidadeConsumo.getDescricao()
        ));
    }
}
