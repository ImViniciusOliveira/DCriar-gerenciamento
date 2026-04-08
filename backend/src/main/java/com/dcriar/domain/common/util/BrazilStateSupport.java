package com.dcriar.domain.common.util;

import java.util.List;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regras de normalização, validação e apresentação para estados brasileiros.
 */
public final class BrazilStateSupport {

    private static final Map<String, StateDefinition> EXACT_LOOKUP = new LinkedHashMap<>();
    private static final Map<String, StateDefinition> STATES_BY_STORAGE_NAME = new LinkedHashMap<>();

    static {
        register("AC", "Acre", "Acre");
        register("AL", "Alagoas", "Alagoas");
        register("AP", "Amapa", "Amapá");
        register("AM", "Amazonas", "Amazonas");
        register("BA", "Bahia", "Bahia");
        register("CE", "Ceara", "Ceará");
        register("DF", "Distrito Federal", "Distrito Federal");
        register("ES", "Espirito Santo", "Espírito Santo");
        register("GO", "Goias", "Goiás");
        register("MA", "Maranhao", "Maranhão");
        register("MT", "Mato Grosso", "Mato Grosso");
        register("MS", "Mato Grosso do Sul", "Mato Grosso do Sul");
        register("MG", "Minas Gerais", "Minas Gerais");
        register("PA", "Para", "Pará");
        register("PB", "Paraiba", "Paraíba");
        register("PR", "Parana", "Paraná");
        register("PE", "Pernambuco", "Pernambuco");
        register("PI", "Piaui", "Piauí");
        register("RJ", "Rio de Janeiro", "Rio de Janeiro");
        register("RN", "Rio Grande do Norte", "Rio Grande do Norte");
        register("RS", "Rio Grande do Sul", "Rio Grande do Sul");
        register("RO", "Rondonia", "Rondônia");
        register("RR", "Roraima", "Roraima");
        register("SC", "Santa Catarina", "Santa Catarina");
        register("SP", "Sao Paulo", "São Paulo");
        register("SE", "Sergipe", "Sergipe");
        register("TO", "Tocantins", "Tocantins");
    }

    private BrazilStateSupport() {
    }

    public static boolean isValid(String value) {
        return resolve(value) != null;
    }

    /**
     * Retorna o nome canônico usado para persistência.
     */
    public static String normalize(String value) {
        StateDefinition state = resolve(value);
        return state == null ? null : state.storageName();
    }

    /**
     * Retorna o nome de exibição acentuado e capitalizado.
     */
    public static String toDisplayName(String value) {
        StateDefinition state = resolve(value);
        return state == null ? null : state.displayName();
    }

    /**
     * Retorna a UF para exibição.
     */
    public static String toUf(String value) {
        StateDefinition state = resolve(value);
        return state == null ? null : state.uf();
    }

    public static List<StateOption> listOptions() {
        return STATES_BY_STORAGE_NAME.values().stream()
                .map(state -> new StateOption(state.uf(), state.displayName()))
                .toList();
    }

    private static StateDefinition resolve(String value) {
        String normalizedInput = UniqueComparisonNormalizer.normalizeCatalogKey(value);
        if (normalizedInput == null) {
            return null;
        }

        StateDefinition exact = EXACT_LOOKUP.get(normalizedInput);
        if (exact != null) {
            return exact;
        }

        return resolveUniquePrefix(normalizedInput, STATES_BY_STORAGE_NAME.values());
    }

    private static StateDefinition resolveUniquePrefix(String normalizedInput, Collection<StateDefinition> states) {
        StateDefinition match = null;

        for (StateDefinition state : states) {
            if (state.normalizedUf().startsWith(normalizedInput)
                    || state.normalizedStorageName().startsWith(normalizedInput)
                    || state.normalizedDisplayName().startsWith(normalizedInput)) {
                if (match != null && !match.equals(state)) {
                    return null;
                }
                match = state;
            }
        }

        return match;
    }

    private static void register(String uf, String storageName, String displayName) {
        StateDefinition state = new StateDefinition(
                uf,
                storageName,
                displayName,
                UniqueComparisonNormalizer.normalizeCatalogKey(uf),
                UniqueComparisonNormalizer.normalizeCatalogKey(storageName),
                UniqueComparisonNormalizer.normalizeCatalogKey(displayName)
        );

        EXACT_LOOKUP.put(state.normalizedUf(), state);
        EXACT_LOOKUP.put(state.normalizedStorageName(), state);
        EXACT_LOOKUP.put(state.normalizedDisplayName(), state);
        STATES_BY_STORAGE_NAME.put(state.normalizedStorageName(), state);
    }

    private record StateDefinition(
            String uf,
            String storageName,
            String displayName,
            String normalizedUf,
            String normalizedStorageName,
            String normalizedDisplayName
    ) {
    }

    public record StateOption(String uf, String displayName) {
    }
}
