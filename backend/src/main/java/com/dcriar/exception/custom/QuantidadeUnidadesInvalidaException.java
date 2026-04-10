package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exceção lançada quando a quantidade de unidades base para um cálculo (como o de custo)
 * é menor ou igual a zero, o que tornaria a operação inválida.
 */
@Getter
public class QuantidadeUnidadesInvalidaException extends RuntimeException {

    private final BigDecimal totalUnidadesBase;

    /**
     * Constrói a exceção com uma mensagem de erro padrão.
     */
    public QuantidadeUnidadesInvalidaException() {
        super("A quantidade total de unidades base para cálculo de custo deve ser maior que zero.");
        this.totalUnidadesBase = null;
    }

    public QuantidadeUnidadesInvalidaException(BigDecimal totalUnidadesBase) {
        super(String.format(
                "A quantidade total de unidades base para cálculo de custo deve ser maior que zero. Valor calculado: %s.",
                HumanNumberDisplayFormatter.formatQuantity(totalUnidadesBase)
        ));
        this.totalUnidadesBase = totalUnidadesBase;
    }
}
