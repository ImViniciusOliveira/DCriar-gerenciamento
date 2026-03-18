package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;

/**
 * Exceção lançada quando o cadastro de um lote informa
 * uma unidade de estoque incompatível com a unidade principal da matéria-prima.
 */
public class UnidadeEstoqueLoteInvalidaException extends RuntimeException {

    private UnidadeEstoqueLoteInvalidaException(String message) {
        super(message);
    }

    public static UnidadeEstoqueLoteInvalidaException unidadeIncompativel(
            UnidadeDeMedida unidadeMateriaPrima,
            UnidadeDeMedida unidadeEstoqueInformada
    ) {
        UnidadeDeMedida unidadeMenor = unidadeMateriaPrima.getUnidadeMenorCompativelParaCadastro();
        if (unidadeMenor != null) {
            return new UnidadeEstoqueLoteInvalidaException(String.format(
                    "A unidade de estoque informada para o lote ('%s') é incompatível com a unidade principal da matéria-prima ('%s'). Para essa matéria-prima, o lote só pode usar '%s' ou '%s'.",
                    unidadeEstoqueInformada,
                    unidadeMateriaPrima,
                    unidadeMateriaPrima,
                    unidadeMenor
            ));
        }

        return new UnidadeEstoqueLoteInvalidaException(String.format(
                "A unidade de estoque informada para o lote ('%s') é incompatível com a unidade principal da matéria-prima ('%s').",
                unidadeEstoqueInformada,
                unidadeMateriaPrima
        ));
    }
}
