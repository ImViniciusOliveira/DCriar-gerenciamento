package com.dcriar.domain.common.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

import java.text.Normalizer;

/**
 * Utilitários para buscas textuais no PostgreSQL com suporte a acentuação.
 * Centraliza a normalização do termo buscado e a expressão SQL baseada em {@code unaccent}.
 */
public final class PostgresSearchUtils {

    private PostgresSearchUtils() {
    }

    public static Expression<String> unaccentedLower(CriteriaBuilder builder, Expression<String> field) {
        return builder.function("unaccent", String.class, builder.lower(field));
    }

    public static String likeTerm(String value) {
        return "%" + normalize(value) + "%";
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase();
    }
}
