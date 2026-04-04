package com.dcriar.domain.common.util;

/**
 * Utilitário para remover espaços acidentais nas extremidades de textos de entrada.
 * <p>
 * Regras aplicadas:
 * - remove espaços no começo e no fim;
 * - retorna {@code null} quando o resultado fica vazio.
 */
public final class TrimTextNormalizer {

    private TrimTextNormalizer() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
