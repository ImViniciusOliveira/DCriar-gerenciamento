ALTER TABLE lotes_materia_prima
    ADD COLUMN unidade_cadastro_estoque VARCHAR(30);

UPDATE lotes_materia_prima
SET unidade_cadastro_estoque = unidade_de_estoque
WHERE unidade_cadastro_estoque IS NULL;

ALTER TABLE lotes_materia_prima
    ALTER COLUMN unidade_cadastro_estoque SET NOT NULL;
