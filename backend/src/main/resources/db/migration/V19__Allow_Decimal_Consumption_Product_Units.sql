ALTER TABLE produtos
    ALTER COLUMN unidades_por_produto TYPE NUMERIC(14,4)
    USING unidades_por_produto::NUMERIC(14,4);
