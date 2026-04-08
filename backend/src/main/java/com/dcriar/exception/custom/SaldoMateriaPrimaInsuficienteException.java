package com.dcriar.exception.custom;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exceção lançada quando uma operação de produção (corte ou consumo) não pode ser
 * concluída por falta de matéria-prima em um ou mais lotes.
 * <p>
 * Esta exceção carrega um contexto detalhado sobre a falha, permitindo a criação
 * de mensagens de erro claras e informativas.
 */
@Getter
public class SaldoMateriaPrimaInsuficienteException extends RuntimeException {

    private final String identificadorLote;
    private final String nomeTipoMateriaPrima;

    /**
     * A quantidade de matéria-prima que a operação tentou consumir.
     */
    private final BigDecimal quantidadeRequisitada;

    /**
     * A quantidade de matéria-prima que estava realmente disponível no momento da falha.
     */
    private final BigDecimal saldoDisponivel;

    /**
     * Constrói a exceção com os detalhes da falha de estoque de matéria-prima.
     *
     * @param quantidadeRequisitada A quantidade que se tentou consumir.
     * @param saldoDisponivel       A quantidade que estava disponível.
     */
    public SaldoMateriaPrimaInsuficienteException(
            String identificadorLote,
            String nomeTipoMateriaPrima,
            BigDecimal quantidadeRequisitada,
            BigDecimal saldoDisponivel
    ) {
        super(String.format(
                "Saldo insuficiente no lote '%s' da matéria-prima '%s'. Necessário: %.2f. Disponível: %.2f.",
                identificadorLote,
                nomeTipoMateriaPrima,
                quantidadeRequisitada,
                saldoDisponivel
        ));
        this.identificadorLote = identificadorLote;
        this.nomeTipoMateriaPrima = nomeTipoMateriaPrima;
        this.quantidadeRequisitada = quantidadeRequisitada;
        this.saldoDisponivel = saldoDisponivel;
    }
}
