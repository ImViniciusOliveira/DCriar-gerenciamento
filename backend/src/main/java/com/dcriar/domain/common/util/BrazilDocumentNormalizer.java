package com.dcriar.domain.common.util;

/**
 * Utilitário para normalização e validação básica de documentos brasileiros usados na venda.
 */
public final class BrazilDocumentNormalizer {

    private BrazilDocumentNormalizer() {
    }

    public static String digitsOnly(String value) {
        String trimmed = TrimTextNormalizer.trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        String digits = trimmed.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    public static String normalizeCpf(String value) {
        return digitsOnly(value);
    }

    public static String normalizeCep(String value) {
        return digitsOnly(value);
    }

    public static boolean isValidCpf(String value) {
        String cpf = normalizeCpf(value);
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }

        return calculateCpfDigit(cpf, 9) == Character.getNumericValue(cpf.charAt(9))
                && calculateCpfDigit(cpf, 10) == Character.getNumericValue(cpf.charAt(10));
    }

    public static boolean isValidCep(String value) {
        String cep = normalizeCep(value);
        return cep == null || cep.length() == 8;
    }

    private static int calculateCpfDigit(String cpf, int length) {
        int sum = 0;
        int weight = length + 1;

        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(cpf.charAt(index)) * (weight - index);
        }

        int remainder = 11 - (sum % 11);
        return remainder >= 10 ? 0 : remainder;
    }
}
