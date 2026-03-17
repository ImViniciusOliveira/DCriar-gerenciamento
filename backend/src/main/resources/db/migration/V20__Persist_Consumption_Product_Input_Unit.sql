ALTER TABLE produtos
    ADD COLUMN unidade_cadastro_consumo VARCHAR(50);

UPDATE produtos
SET unidade_cadastro_consumo = tipo_materia_prima.unidade_de_consumo
FROM tipos_materia_prima tipo_materia_prima
WHERE produtos.tipo_produto = 'CONSUMO'
  AND produtos.tipo_materia_prima_id = tipo_materia_prima.id
  AND produtos.unidade_cadastro_consumo IS NULL;
