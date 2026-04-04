CREATE UNIQUE INDEX uq_canais_venda_nome_ci
    ON canais_venda (LOWER(TRIM(nome)));
