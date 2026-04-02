package com.dcriar.domain.stock.model;

import java.math.BigDecimal;

/**
 * Representa a valoração operacional atual de um lote, separando saldo remanescente,
 * valor econômico do saldo e custo unitário usado na exibição e nos cálculos de ajuste.
 */
public record ValorizacaoAtualLoteMateriaPrima(
        BigDecimal saldoInterno,
        BigDecimal saldoApresentacao,
        BigDecimal valorAtualLote,
        BigDecimal custoUnitarioAtualInterno,
        BigDecimal custoUnitarioAtualApresentacao
) {
}
