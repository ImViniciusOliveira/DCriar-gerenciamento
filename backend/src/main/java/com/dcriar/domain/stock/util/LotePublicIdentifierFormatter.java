package com.dcriar.domain.stock.util;

import com.dcriar.domain.stock.entity.LoteMateriaPrima;

import java.util.List;

/**
 * Centraliza a formatação de identificadores públicos de lotes e retalhos.
 */
public final class LotePublicIdentifierFormatter {

    private static final String PREFIXO_LOTE_PRINCIPAL = "LT";
    private static final String PREFIXO_RETALHO = "RT";

    private LotePublicIdentifierFormatter() {
    }

    public static String format(LoteMateriaPrima lote) {
        return format(lote.getId(), lote.getLoteDeOrigem() != null);
    }

    public static String format(Long loteId, boolean retalho) {
        if (loteId == null) {
            return null;
        }
        return (retalho ? PREFIXO_RETALHO : PREFIXO_LOTE_PRINCIPAL) + "-" + String.format("%06d", loteId);
    }

    public static String resolverTipoEstrutural(LoteMateriaPrima lote) {
        return lote.getLoteDeOrigem() == null ? "LOTE_PRINCIPAL" : "RETALHO";
    }

    public static String formatarCadeia(List<LoteMateriaPrima> cadeia) {
        return cadeia.stream()
                .map(LotePublicIdentifierFormatter::format)
                .reduce((atual, proximo) -> atual + " -> " + proximo)
                .orElse(null);
    }
}
