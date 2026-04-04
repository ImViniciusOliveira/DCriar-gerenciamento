package com.dcriar.domain.common.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class PostgresNormalizedUniquenessChecker implements NormalizedUniquenessChecker {

    private static final String ID_COLUMN = "id";

    private static final String CANAIS_VENDA = "canais_venda";
    private static final String TIPOS_MATERIA_PRIMA = "tipos_materia_prima";
    private static final String PRODUTOS = "produtos";

    private static final String NOME = "nome";
    private static final String SKU = "sku";

    private static final String CATALOG_FUNCTION = "dcriar_normalize_catalog_key";
    private static final String TRIMMED_FUNCTION = "dcriar_normalize_trimmed_key";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean existsCanalVendaNome(String nome) {
        return exists(CANAIS_VENDA, NOME, CATALOG_FUNCTION, nome, null);
    }

    @Override
    public boolean existsCanalVendaNome(Long excludingId, String nome) {
        return exists(CANAIS_VENDA, NOME, CATALOG_FUNCTION, nome, excludingId);
    }

    @Override
    public boolean existsTipoMateriaPrimaNome(String nome) {
        return exists(TIPOS_MATERIA_PRIMA, NOME, CATALOG_FUNCTION, nome, null);
    }

    @Override
    public boolean existsTipoMateriaPrimaNome(Long excludingId, String nome) {
        return exists(TIPOS_MATERIA_PRIMA, NOME, CATALOG_FUNCTION, nome, excludingId);
    }

    @Override
    public boolean existsProdutoNome(String nome) {
        return exists(PRODUTOS, NOME, CATALOG_FUNCTION, nome, null);
    }

    @Override
    public boolean existsProdutoNome(Long excludingId, String nome) {
        return exists(PRODUTOS, NOME, CATALOG_FUNCTION, nome, excludingId);
    }

    @Override
    public boolean existsProdutoSku(String sku) {
        return exists(PRODUTOS, SKU, TRIMMED_FUNCTION, sku, null);
    }

    @Override
    public boolean existsProdutoSku(Long excludingId, String sku) {
        return exists(PRODUTOS, SKU, TRIMMED_FUNCTION, sku, excludingId);
    }

    private boolean exists(String tableName, String columnName, String normalizationFunction, String value, Long excludingId) {
        StringBuilder sql = new StringBuilder("""
                SELECT EXISTS (
                    SELECT 1
                    FROM %s t
                    WHERE %s(t.%s) = %s(:value)
                """.formatted(tableName, normalizationFunction, columnName, normalizationFunction));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", value);

        if (excludingId != null) {
            sql.append(" AND t.").append(ID_COLUMN).append(" <> :excludingId");
            parameters.put("excludingId", excludingId);
        }

        sql.append(")");

        Query query = entityManager.createNativeQuery(sql.toString());
        parameters.forEach(query::setParameter);

        return toBoolean(query.getSingleResult());
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
