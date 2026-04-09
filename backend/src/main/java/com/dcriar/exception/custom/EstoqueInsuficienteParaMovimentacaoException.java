package com.dcriar.exception.custom;

import lombok.Getter;

/**
 * Exceção lançada quando uma movimentação de estoque em um {@link com.dcriar.domain.stock.entity.LoteMateriaPrima}
 * não pode ser concluída por falta de saldo disponível.
 * <p>
 * Esta é uma exceção crítica para a integridade do estoque, garantindo que o saldo
 * de um lote nunca se torne negativo.
 */
@Getter
public class EstoqueInsuficienteParaMovimentacaoException extends RuntimeException {

    /**
     * O ID do lote no qual a movimentação foi tentada.
     */
    private final Long loteId;
    private final String identificadorPublicoLote;
    private final String nomeTipoMateriaPrima;
    private final String unidadeApresentacao;

    /**
     * A quantidade (em kg, m, etc.) que a operação tentou movimentar.
     */
    private final double quantidadeRequisitada;

    /**
     * O saldo que estava realmente disponível no lote no momento da falha.
     */
    private final double saldoDisponivel;

    /**
     * Constrói a exceção com todos os detalhes da falha de estoque no lote.
     *
     * @param loteId ID do lote no qual a movimentação foi tentada.
     * @param quantidadeRequisitada Quantidade que se tentou registrar.
     * @param saldoDisponivel Saldo disponível no lote no momento da falha.
     */
    public EstoqueInsuficienteParaMovimentacaoException(
            Long loteId,
            String identificadorPublicoLote,
            String nomeTipoMateriaPrima,
            String unidadeApresentacao,
            double quantidadeRequisitada,
            double saldoDisponivel
    ) {
        super(String.format(
                "Não é possível registrar a movimentação no lote '%s' da matéria-prima '%s'. Foram solicitadas %s, mas o saldo disponível é %s.",
                identificadorPublicoLote,
                nomeTipoMateriaPrima,
                formatarQuantidade(Math.abs(quantidadeRequisitada), unidadeApresentacao),
                formatarQuantidade(saldoDisponivel, unidadeApresentacao)
        ));
        this.loteId = loteId;
        this.identificadorPublicoLote = identificadorPublicoLote;
        this.nomeTipoMateriaPrima = nomeTipoMateriaPrima;
        this.unidadeApresentacao = unidadeApresentacao;
        this.quantidadeRequisitada = quantidadeRequisitada;
        this.saldoDisponivel = saldoDisponivel;
    }

    private static String formatarQuantidade(double quantidade, String unidadeApresentacao) {
        String quantidadeFormatada = String.format("%.4f", quantidade);
        if (unidadeApresentacao == null || unidadeApresentacao.isBlank()) {
            return quantidadeFormatada + " unidades";
        }
        return quantidadeFormatada + " " + unidadeApresentacao;
    }
}
