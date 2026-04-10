package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exceção lançada quando um valor numérico excede o limite máximo
 * permitido pela definição do banco de dados ou pela regra de negócio.
 * <p>
 * Esta exceção é usada para tratar casos onde o cálculo interno (como custo por unidade)
 * gera um valor maior do que o sistema pode suportar.
 */
@Getter
public class ValorNumericoExcedeLimiteException extends RuntimeException {

    private final String nomeDoCampo;
    private final String valorEnviado;
    private final String limiteMaximo;

    /**
     * Constrói a exceção com todos os detalhes do erro.
     *
     * @param nomeDoCampo    O nome do campo que excedeu o limite.
     * @param valorEnviado   O valor que causou o estouro.
     * @param limiteMaximo   O limite máximo que o campo suporta.
     */
    public ValorNumericoExcedeLimiteException(String nomeDoCampo, BigDecimal valorEnviado, int limiteInteiro) {
        super(String.format(
                "O valor calculado para '%s' (%s) excede o limite de %d dígitos inteiros.",
                nomeDoCampo,
                HumanNumberDisplayFormatter.summarize(valorEnviado, 4, 20),
                limiteInteiro
        ));
        this.nomeDoCampo = nomeDoCampo;
        this.valorEnviado = valorEnviado.toPlainString();
        this.limiteMaximo = limiteInteiro + " dígitos inteiros";
    }
}
