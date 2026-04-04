package com.dcriar.domain.common.util;

import java.util.regex.Pattern;

/**
 * Utilitário para normalização de nomes e textos de catálogo.
 * <p>
 * Regras aplicadas:
 * - remove espaços nas extremidades;
 * - colapsa múltiplos espaços internos em um único espaço;
 * - retorna {@code null} quando o resultado fica vazio.
 * <p>
 * Deve ser usado apenas em campos de catálogo onde múltiplos espaços internos
 * não fazem parte do valor desejado.
 */
public final class HumanTextNormalizer {

    private static final Pattern MULTIPLE_WHITESPACE = Pattern.compile("\\s+");

    private HumanTextNormalizer() {
    }

    public static String normalize(String value) {
        String trimmed = TrimTextNormalizer.trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        return MULTIPLE_WHITESPACE.matcher(trimmed).replaceAll(" ");
    }
}
