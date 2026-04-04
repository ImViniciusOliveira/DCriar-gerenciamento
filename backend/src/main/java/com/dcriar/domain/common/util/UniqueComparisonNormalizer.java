package com.dcriar.domain.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normaliza textos para comparação de unicidade sem diferenciar caixa nem acentuação.
 * <p>
 * O valor persistido continua sendo o original do usuário; esta classe só deve ser usada
 * para comparar chaves lógicas de unicidade.
 */
public final class UniqueComparisonNormalizer {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private UniqueComparisonNormalizer() {
    }

    public static String normalizeCatalogKey(String value) {
        return normalizeComparable(HumanTextNormalizer.normalize(value));
    }

    public static String normalizeTrimmedKey(String value) {
        return normalizeComparable(TrimTextNormalizer.trimToNull(value));
    }

    public static boolean equalsCatalogKey(String left, String right) {
        return equalsNormalized(normalizeCatalogKey(left), normalizeCatalogKey(right));
    }

    public static boolean equalsTrimmedKey(String left, String right) {
        return equalsNormalized(normalizeTrimmedKey(left), normalizeTrimmedKey(right));
    }

    private static String normalizeComparable(String value) {
        if (value == null) {
            return null;
        }

        String withoutAccents = COMBINING_MARKS.matcher(
                Normalizer.normalize(value, Normalizer.Form.NFD)
        ).replaceAll("");

        return withoutAccents.toLowerCase(Locale.ROOT);
    }

    private static boolean equalsNormalized(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }
}
