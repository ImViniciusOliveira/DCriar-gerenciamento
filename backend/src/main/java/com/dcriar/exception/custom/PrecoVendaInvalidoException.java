package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exceção lançada quando os valores monetários enviados para um item de venda
 * não respeitam as regras de consistência do modelo comercial.
 */
@Getter
public class PrecoVendaInvalidoException extends RuntimeException {

    private final String codigo;
    private final BigDecimal precoComercialOriginal;
    private final BigDecimal precoAplicado;

    private PrecoVendaInvalidoException(
            String codigo,
            BigDecimal precoComercialOriginal,
            BigDecimal precoAplicado,
            String message
    ) {
        super(message);
        this.codigo = codigo;
        this.precoComercialOriginal = precoComercialOriginal;
        this.precoAplicado = precoAplicado;
    }

    public static PrecoVendaInvalidoException precoPadraoDivergente(BigDecimal precoComercialOriginal, BigDecimal precoAplicado) {
        return new PrecoVendaInvalidoException(
                "PRECO_PADRAO_DIVERGENTE",
                precoComercialOriginal,
                precoAplicado,
                "O item marcado como preço padrão deve usar exatamente o preço comercial atual do produto. Esperado: " +
                        HumanNumberDisplayFormatter.formatMoney(precoComercialOriginal) +
                        ". Informado: " +
                        HumanNumberDisplayFormatter.formatMoney(precoAplicado) +
                        "."
        );
    }

}
