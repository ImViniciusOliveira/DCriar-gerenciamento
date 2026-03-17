package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;

/**
 * Exceção lançada quando o cadastro de um produto de consumo informa
 * uma unidade incompatível com a unidade principal da matéria-prima.
 */
public class UnidadeCadastroConsumoInvalidaException extends RuntimeException {

    private UnidadeCadastroConsumoInvalidaException(String message) {
        super(message);
    }

    public static UnidadeCadastroConsumoInvalidaException unidadeIncompativel(
            UnidadeDeMedida unidadeMateriaPrima,
            UnidadeDeMedida unidadeInformada
    ) {
        UnidadeDeMedida unidadeMenor = unidadeMateriaPrima.getUnidadeMenorCompativelParaCadastro();
        if (unidadeMenor != null) {
            return new UnidadeCadastroConsumoInvalidaException(String.format(
                    "A unidade informada para cadastro de consumo ('%s') é incompatível com a unidade principal da matéria-prima ('%s'). Use '%s' ou '%s'.",
                    unidadeInformada,
                    unidadeMateriaPrima,
                    unidadeMateriaPrima,
                    unidadeMenor
            ));
        }

        return new UnidadeCadastroConsumoInvalidaException(String.format(
                "A unidade informada para cadastro de consumo ('%s') é incompatível com a unidade principal da matéria-prima ('%s').",
                unidadeInformada,
                unidadeMateriaPrima
        ));
    }
}
