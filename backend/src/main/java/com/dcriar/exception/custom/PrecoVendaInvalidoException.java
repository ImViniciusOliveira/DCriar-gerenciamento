package com.dcriar.exception.custom;

import java.math.BigDecimal;

/**
 * Exceção lançada quando os valores monetários enviados para um item de venda
 * não respeitam as regras de consistência do modelo comercial.
 */
public class PrecoVendaInvalidoException extends RuntimeException {

    private PrecoVendaInvalidoException(String message) {
        super(message);
    }

    public static PrecoVendaInvalidoException precoPadraoDivergente(BigDecimal precoComercialOriginal, BigDecimal precoAplicado) {
        return new PrecoVendaInvalidoException(String.format(
                "O item marcado como preço padrão deve usar exatamente o preço comercial atual do produto. Esperado: %s. Informado: %s.",
                precoComercialOriginal,
                precoAplicado
        ));
    }

}
