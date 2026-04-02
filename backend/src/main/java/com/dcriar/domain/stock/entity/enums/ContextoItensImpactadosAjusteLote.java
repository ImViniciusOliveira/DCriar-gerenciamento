package com.dcriar.domain.stock.entity.enums;

/**
 * Contexto semântico do resultado de itens impactados em um ajuste operacional de lote.
 */
public enum ContextoItensImpactadosAjusteLote {
    COM_ITENS_IMPACTADOS,
    PERDA_NAO_RECALCULA_DERIVADOS,
    MATERIA_PRIMA_NAO_GERA_RETALHO,
    SEM_RETALHOS_VINCULADOS,
    SEM_RETALHOS_COM_SALDO
}
