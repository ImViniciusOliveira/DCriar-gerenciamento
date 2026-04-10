package com.dcriar.domain.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formata números para exibição humana no padrão pt-BR, removendo zeros desnecessários
 * e preservando precisão máxima adequada por contexto.
 */
public final class HumanNumberDisplayFormatter {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private HumanNumberDisplayFormatter() {
    }

    public static String formatInteger(Number value) {
        if (value == null) {
            return null;
        }
        return createFormatter(0).format(value.longValue());
    }

    public static String formatCount(Number value) {
        return formatInteger(value);
    }

    public static String formatQuantity(BigDecimal value) {
        return format(value, 4);
    }

    public static String formatQuantity(double value) {
        return formatQuantity(BigDecimal.valueOf(value));
    }

    public static String formatMoney(BigDecimal value) {
        return format(value, 2);
    }

    public static String formatLengthCm(BigDecimal value) {
        return format(value, 2) + "cm";
    }

    public static String formatLengthCm(double value) {
        return formatLengthCm(BigDecimal.valueOf(value));
    }

    public static String formatDimensionCm(BigDecimal largura, BigDecimal comprimento) {
        return format(largura, 2) + "cm x " + format(comprimento, 2) + "cm";
    }

    public static String formatQuantityWithUnit(BigDecimal value, String unit) {
        String formatted = formatQuantity(value);
        if (unit == null || unit.isBlank()) {
            return formatted + " unidades";
        }
        return formatted + unit;
    }

    public static String formatQuantityWithUnit(double value, String unit) {
        return formatQuantityWithUnit(BigDecimal.valueOf(value), unit);
    }

    public static String format(BigDecimal value, int maxFractionDigits) {
        if (value == null) {
            return null;
        }

        BigDecimal normalized = value.stripTrailingZeros();
        DecimalFormat formatter = createFormatter(maxFractionDigits);
        return formatter.format(normalized);
    }

    public static String summarize(BigDecimal value, int maxFractionDigits, int maxLength) {
        String formatted = format(value, maxFractionDigits);
        if (formatted == null || formatted.length() <= maxLength) {
            return formatted;
        }
        return formatted.substring(0, maxLength) + "...";
    }

    private static DecimalFormat createFormatter(int maxFractionDigits) {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(PT_BR);
        formatter.setGroupingUsed(true);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(maxFractionDigits);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter;
    }
}
