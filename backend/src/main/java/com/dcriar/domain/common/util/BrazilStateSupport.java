package com.dcriar.domain.common.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regras de normalização e validação para estados brasileiros.
 */
public final class BrazilStateSupport {

    private static final Map<String, String> STATES = new LinkedHashMap<>();

    static {
        register("AC", "Acre");
        register("AL", "Alagoas");
        register("AP", "Amapa");
        register("AM", "Amazonas");
        register("BA", "Bahia");
        register("CE", "Ceara");
        register("DF", "Distrito Federal");
        register("ES", "Espirito Santo");
        register("GO", "Goias");
        register("MA", "Maranhao");
        register("MT", "Mato Grosso");
        register("MS", "Mato Grosso do Sul");
        register("MG", "Minas Gerais");
        register("PA", "Para");
        register("PB", "Paraiba");
        register("PR", "Parana");
        register("PE", "Pernambuco");
        register("PI", "Piaui");
        register("RJ", "Rio de Janeiro");
        register("RN", "Rio Grande do Norte");
        register("RS", "Rio Grande do Sul");
        register("RO", "Rondonia");
        register("RR", "Roraima");
        register("SC", "Santa Catarina");
        register("SP", "Sao Paulo");
        register("SE", "Sergipe");
        register("TO", "Tocantins");
    }

    private BrazilStateSupport() {
    }

    public static boolean isValid(String value) {
        return normalize(value) != null;
    }

    public static String normalize(String value) {
        String normalizedInput = UniqueComparisonNormalizer.normalizeCatalogKey(value);
        if (normalizedInput == null) {
            return null;
        }

        return STATES.get(normalizedInput);
    }

    private static void register(String uf, String canonicalName) {
        STATES.put(UniqueComparisonNormalizer.normalizeCatalogKey(uf), canonicalName);
        STATES.put(UniqueComparisonNormalizer.normalizeCatalogKey(canonicalName), canonicalName);
    }
}
