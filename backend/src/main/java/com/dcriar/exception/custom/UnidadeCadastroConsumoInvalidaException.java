package com.dcriar.exception.custom;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import lombok.Getter;

/**
 * Exceção lançada quando o cadastro de um produto de consumo informa
 * uma unidade incompatível com a unidade da matéria-prima
 * ou com uma subdivisão compatível desse mesmo grupo.
 */
@Getter
public class UnidadeCadastroConsumoInvalidaException extends RuntimeException {

    private final UnidadeDeMedida unidadeMateriaPrima;
    private final UnidadeDeMedida unidadeInformada;
    private final UnidadeDeMedida unidadeMenorCompativel;

    private UnidadeCadastroConsumoInvalidaException(
            UnidadeDeMedida unidadeMateriaPrima,
            UnidadeDeMedida unidadeInformada,
            UnidadeDeMedida unidadeMenorCompativel,
            String message
    ) {
        super(message);
        this.unidadeMateriaPrima = unidadeMateriaPrima;
        this.unidadeInformada = unidadeInformada;
        this.unidadeMenorCompativel = unidadeMenorCompativel;
    }

    public static UnidadeCadastroConsumoInvalidaException unidadeIncompativel(
            UnidadeDeMedida unidadeMateriaPrima,
            UnidadeDeMedida unidadeInformada
    ) {
        UnidadeDeMedida unidadeMenor = unidadeMateriaPrima.getUnidadeMenorCompativelParaCadastro();
        if (unidadeMenor != null) {
            return new UnidadeCadastroConsumoInvalidaException(
                    unidadeMateriaPrima,
                    unidadeInformada,
                    unidadeMenor,
                    String.format(
                            "A unidade informada para cadastro de consumo ('%s') é incompatível com a unidade da matéria-prima ('%s'). Use '%s' ou uma subdivisão compatível como '%s'.",
                            unidadeInformada,
                            unidadeMateriaPrima,
                            unidadeMateriaPrima,
                            unidadeMenor
                    )
            );
        }

        return new UnidadeCadastroConsumoInvalidaException(
                unidadeMateriaPrima,
                unidadeInformada,
                null,
                String.format(
                        "A unidade informada para cadastro de consumo ('%s') é incompatível com a unidade da matéria-prima ('%s').",
                        unidadeInformada,
                        unidadeMateriaPrima
                )
        );
    }
}
