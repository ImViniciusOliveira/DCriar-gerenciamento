package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
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
    private final String unidadeApresentacao;

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
            String unidadeApresentacao,
            BigDecimal quantidadeRequisitada,
            BigDecimal saldoDisponivel
    ) {
        super("Saldo insuficiente no lote '" + identificadorLote + "' da matéria-prima '" + nomeTipoMateriaPrima +
                "'. Necessário: " + HumanNumberDisplayFormatter.formatQuantityWithUnit(quantidadeRequisitada, unidadeApresentacao) +
                ". Disponível: " + HumanNumberDisplayFormatter.formatQuantityWithUnit(saldoDisponivel, unidadeApresentacao) + ".");
        this.identificadorLote = identificadorLote;
        this.nomeTipoMateriaPrima = nomeTipoMateriaPrima;
        this.unidadeApresentacao = unidadeApresentacao;
        this.quantidadeRequisitada = quantidadeRequisitada;
        this.saldoDisponivel = saldoDisponivel;
    }
}
