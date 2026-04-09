package com.dcriar.domain.common.util;

import java.math.BigDecimal;

public final class StatusDivergenciaEstoqueUtils {

    public static final String CONSISTENTE = "CONSISTENTE";
    public static final String INCONSISTENTE = "INCONSISTENTE";

    private StatusDivergenciaEstoqueUtils() {
    }

    public static String resolverParaProduto(Integer estoqueFisicoTotal, Integer estoqueDistribuidoTotal, Integer estoqueDisponivelParaAlocar) {
        if ((estoqueFisicoTotal != null && estoqueFisicoTotal < 0)
                || (estoqueDisponivelParaAlocar != null && estoqueDisponivelParaAlocar < 0)
                || (estoqueFisicoTotal != null && estoqueDistribuidoTotal != null && estoqueDistribuidoTotal > estoqueFisicoTotal)) {
            return INCONSISTENTE;
        }

        return CONSISTENTE;
    }

    public static String resolverParaCanal(Integer quantidadeNoCanal, Integer estoqueFisicoTotal, Integer estoqueDistribuidoTotal, Integer estoqueDisponivelParaAlocar) {
        if ((quantidadeNoCanal != null && quantidadeNoCanal < 0)
                || INCONSISTENTE.equals(resolverParaProduto(estoqueFisicoTotal, estoqueDistribuidoTotal, estoqueDisponivelParaAlocar))) {
            return INCONSISTENTE;
        }

        return CONSISTENTE;
    }

    public static String resolverParaLote(BigDecimal saldoEstoque) {
        if (saldoEstoque != null && saldoEstoque.compareTo(BigDecimal.ZERO) < 0) {
            return INCONSISTENTE;
        }

        return CONSISTENTE;
    }
}
