package com.dcriar.domain.common.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formata textos humanos para exibição, preservando um padrão legível no retorno da API.
 */
public final class HumanTextDisplayFormatter {

    private static final Set<String> LOWERCASE_WORDS = Set.of("de", "da", "do", "das", "dos", "e");

    private HumanTextDisplayFormatter() {
    }

    public static String format(String value) {
        String normalized = HumanTextNormalizer.normalize(value);
        if (normalized == null) {
            return null;
        }

        return Arrays.stream(normalized.split("\\s+"))
                .map(HumanTextDisplayFormatter::formatWord)
                .collect(Collectors.joining(" "));
    }

    private static String formatWord(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (LOWERCASE_WORDS.contains(lower)) {
            return lower;
        }

        if ("sao".equals(lower)) {
            return "São";
        }

        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
