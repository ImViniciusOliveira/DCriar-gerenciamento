package com.dcriar.domain.common.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilitário para aplicar trim lateral em chaves e valores string dentro de estruturas de mapa.
 */
public final class MapStringValueTrimmer {

    private MapStringValueTrimmer() {
    }

    public static Map<String, String> trimStringValues(Map<String, String> source) {
        if (source == null) {
            return null;
        }

        Map<String, String> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> normalized.put(
                TrimTextNormalizer.trimToNull(key),
                TrimTextNormalizer.trimToNull(value)
        ));
        return normalized;
    }

    public static Map<String, Object> trimObjectStringValues(Map<String, Object> source) {
        if (source == null) {
            return null;
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        source.forEach((key, value) -> normalized.put(
                TrimTextNormalizer.trimToNull(key),
                normalizeObject(value)
        ));
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeObject(Object value) {
        if (value instanceof String stringValue) {
            return TrimTextNormalizer.trimToNull(stringValue);
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            mapValue.forEach((entryKey, entryValue) -> normalized.put(
                    TrimTextNormalizer.trimToNull(String.valueOf(entryKey)),
                    normalizeObject(entryValue)
            ));
            return normalized;
        }
        if (value instanceof List<?> listValue) {
            return listValue.stream().map(MapStringValueTrimmer::normalizeObject).toList();
        }
        return value;
    }
}
