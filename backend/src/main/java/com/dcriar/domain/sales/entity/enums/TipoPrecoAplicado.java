package com.dcriar.domain.sales.entity.enums;

import com.dcriar.exception.custom.TipoPrecoAplicadoInvalidoException;

/**
 * Representa como o preço de um item de venda foi definido no momento da transação.
 */
public enum TipoPrecoAplicado {
    PRECO_PADRAO,
    PRECO_ALTERADO,
    DESCONTO_TOTAL;

    public static boolean isInvalid(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        for (TipoPrecoAplicado tipo : values()) {
            if (tipo.name().equals(value)) {
                return false;
            }
        }

        return true;
    }

    public static TipoPrecoAplicado from(String value) {
        if (isInvalid(value)) {
            throw new TipoPrecoAplicadoInvalidoException(value);
        }

        return TipoPrecoAplicado.valueOf(value);
    }
}
