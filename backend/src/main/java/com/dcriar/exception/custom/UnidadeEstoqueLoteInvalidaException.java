package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import lombok.Getter;

/**
 * Exceção lançada quando o cadastro de um lote informa
 * uma unidade de estoque incompatível com a unidade da matéria-prima
 * ou com uma subdivisão compatível desse mesmo grupo.
 */
@Getter
public class UnidadeEstoqueLoteInvalidaException extends RuntimeException {

    private final UnidadeDeMedida unidadeMateriaPrima;
    private final UnidadeDeMedida unidadeEstoqueInformada;
    private final UnidadeDeMedida unidadeMenorCompativel;

    private UnidadeEstoqueLoteInvalidaException(
            UnidadeDeMedida unidadeMateriaPrima,
            UnidadeDeMedida unidadeEstoqueInformada,
            UnidadeDeMedida unidadeMenorCompativel,
            String message
    ) {
        super(message);
        this.unidadeMateriaPrima = unidadeMateriaPrima;
        this.unidadeEstoqueInformada = unidadeEstoqueInformada;
        this.unidadeMenorCompativel = unidadeMenorCompativel;
    }

    public static UnidadeEstoqueLoteInvalidaException unidadeIncompativel(
            UnidadeDeMedida unidadeMateriaPrima,
            UnidadeDeMedida unidadeEstoqueInformada
    ) {
        UnidadeDeMedida unidadeMenor = unidadeMateriaPrima.getUnidadeMenorCompativelParaCadastro();
        if (unidadeMenor != null) {
            return new UnidadeEstoqueLoteInvalidaException(
                    unidadeMateriaPrima,
                    unidadeEstoqueInformada,
                    unidadeMenor,
                    String.format(
                    "A unidade de estoque informada para o lote ('%s') é incompatível com a unidade da matéria-prima ('%s'). Para essa matéria-prima, o lote só pode usar '%s' ou uma subdivisão compatível como '%s'.",
                    unidadeEstoqueInformada,
                    unidadeMateriaPrima,
                    unidadeMateriaPrima,
                    unidadeMenor
                    )
            );
        }

        return new UnidadeEstoqueLoteInvalidaException(
                unidadeMateriaPrima,
                unidadeEstoqueInformada,
                null,
                String.format(
                "A unidade de estoque informada para o lote ('%s') é incompatível com a unidade da matéria-prima ('%s').",
                unidadeEstoqueInformada,
                unidadeMateriaPrima
                )
        );
    }
}
