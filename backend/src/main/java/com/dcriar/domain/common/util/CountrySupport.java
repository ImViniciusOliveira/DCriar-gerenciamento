package com.dcriar.domain.common.util;

import com.dcriar.domain.sales.entity.enums.ModoLocalidadeVenda;

/**
 * Regras de normalização e apresentação para país no contexto de vendas.
 */
public final class CountrySupport {

    public static final String DEFAULT_COUNTRY = "Brasil";

    private CountrySupport() {
    }

    public static String normalizeForStorage(String value) {
        String normalized = HumanTextNormalizer.normalize(value);
        if (normalized == null || isBrazil(normalized)) {
            return DEFAULT_COUNTRY;
        }
        return normalized;
    }

    public static String toDisplayName(String value) {
        String normalized = normalizeForStorage(value);
        if (isBrazil(normalized)) {
            return DEFAULT_COUNTRY;
        }
        return HumanTextDisplayFormatter.format(normalized);
    }

    public static boolean isBrazil(String value) {
        return UniqueComparisonNormalizer.equalsCatalogKey(value, DEFAULT_COUNTRY);
    }

    public static ModoLocalidadeVenda resolveLocationMode(String value) {
        return isBrazil(value) ? ModoLocalidadeVenda.BRASIL : ModoLocalidadeVenda.LIVRE;
    }
}
