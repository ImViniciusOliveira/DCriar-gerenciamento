ALTER TABLE itens_venda
    ADD COLUMN preco_comercial_original NUMERIC(19, 2),
    ADD COLUMN tipo_preco_aplicado VARCHAR(30),
    ADD COLUMN motivo_alteracao_preco VARCHAR(255);

UPDATE itens_venda
SET preco_comercial_original = preco_unitario,
    tipo_preco_aplicado = 'PRECO_PADRAO'
WHERE preco_comercial_original IS NULL;

ALTER TABLE itens_venda
    ALTER COLUMN preco_comercial_original SET NOT NULL,
    ALTER COLUMN tipo_preco_aplicado SET NOT NULL;
