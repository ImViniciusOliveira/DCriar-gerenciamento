CREATE OR REPLACE FUNCTION dcriar_unaccent_immutable(value TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
    SELECT public.unaccent('unaccent', value);
$$;

CREATE OR REPLACE FUNCTION dcriar_normalize_catalog_key(value TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
    SELECT LOWER(
        dcriar_unaccent_immutable(
            REGEXP_REPLACE(TRIM(value), '\s+', ' ', 'g')
        )
    );
$$;

CREATE OR REPLACE FUNCTION dcriar_normalize_trimmed_key(value TEXT)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
STRICT
AS $$
    SELECT LOWER(
        dcriar_unaccent_immutable(
            TRIM(value)
        )
    );
$$;

DROP INDEX IF EXISTS uq_canais_venda_nome_ci;

CREATE UNIQUE INDEX IF NOT EXISTS uq_canais_venda_nome_normalized
    ON canais_venda (dcriar_normalize_catalog_key(nome));

CREATE UNIQUE INDEX IF NOT EXISTS uq_tipos_materia_prima_nome_normalized
    ON tipos_materia_prima (dcriar_normalize_catalog_key(nome));

CREATE UNIQUE INDEX IF NOT EXISTS uq_produtos_nome_normalized
    ON produtos (dcriar_normalize_catalog_key(nome));

CREATE UNIQUE INDEX IF NOT EXISTS uq_produtos_sku_normalized
    ON produtos (dcriar_normalize_trimmed_key(sku));
