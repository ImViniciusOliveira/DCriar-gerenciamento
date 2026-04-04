package com.dcriar.domain.common.util;

import com.dcriar.exception.custom.LogicalMapKeyInvalidaException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras de validação e busca para chaves lógicas de mapas flexíveis.
 * <p>
 * As comparações ignoram caixa, acentuação e espaços nas extremidades.
 */
public final class LogicalMapKeySupport {

    private LogicalMapKeySupport() {
    }

    public static void validateNoLogicalDuplicates(Map<?, ?> source, String rootField) {
        if (source == null) {
            return;
        }
        validateMap(source, rootField);
    }

    public static boolean containsLogicalKey(Map<?, ?> source, String expectedKey) {
        return findEntry(source, expectedKey) != null;
    }

    public static Object getLogicalValue(Map<?, ?> source, String expectedKey) {
        Map.Entry<?, ?> entry = findEntry(source, expectedKey);
        return entry != null ? entry.getValue() : null;
    }

    private static void validateMap(Map<?, ?> source, String currentPath) {
        Map<String, String> seenKeys = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String rawKey = entry.getKey() != null ? String.valueOf(entry.getKey()) : null;
            String trimmedKey = TrimTextNormalizer.trimToNull(rawKey);
            if (trimmedKey == null) {
                throw LogicalMapKeyInvalidaException.chaveVazia(currentPath);
            }

            String logicalKey = UniqueComparisonNormalizer.normalizeTrimmedKey(trimmedKey);
            String previousKey = seenKeys.putIfAbsent(logicalKey, trimmedKey);
            if (previousKey != null) {
                throw LogicalMapKeyInvalidaException.chavesEquivalentes(currentPath, previousKey, trimmedKey);
            }

            Object value = entry.getValue();
            String nestedPath = currentPath + "." + trimmedKey;
            if (value instanceof Map<?, ?> nestedMap) {
                validateMap(nestedMap, nestedPath);
            } else if (value instanceof List<?> listValue) {
                validateList(listValue, nestedPath);
            }
        }
    }

    private static void validateList(List<?> source, String currentPath) {
        for (int index = 0; index < source.size(); index++) {
            Object item = source.get(index);
            if (item instanceof Map<?, ?> nestedMap) {
                validateMap(nestedMap, currentPath + "[" + index + "]");
            } else if (item instanceof List<?> nestedList) {
                validateList(nestedList, currentPath + "[" + index + "]");
            }
        }
    }

    private static Map.Entry<?, ?> findEntry(Map<?, ?> source, String expectedKey) {
        if (source == null) {
            return null;
        }

        String logicalExpectedKey = UniqueComparisonNormalizer.normalizeTrimmedKey(expectedKey);
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String rawKey = entry.getKey() != null ? String.valueOf(entry.getKey()) : null;
            if (logicalExpectedKey != null
                    && logicalExpectedKey.equals(UniqueComparisonNormalizer.normalizeTrimmedKey(rawKey))) {
                return entry;
            }
        }
        return null;
    }
}
