-- V11: Adiciona suporte para herança de produtos (Tabela Única)

-- 1. Adiciona a coluna discriminadora para diferenciar os tipos de produto.
-- O valor padrão 'CORTE' será aplicado a todos os produtos existentes.
ALTER TABLE produtos ADD COLUMN tipo_produto VARCHAR(31) NOT NULL DEFAULT 'CORTE';

-- 2. Adiciona as novas colunas que pertencem apenas a produtos de consumo.
ALTER TABLE produtos ADD COLUMN codigo_fabricante VARCHAR(255);
ALTER TABLE produtos ADD COLUMN especificacoes JSONB;

-- 3. Torna as colunas que pertencem apenas a produtos de corte nulas.
-- Isso é necessário porque produtos de consumo não terão esses valores.
ALTER TABLE produtos ALTER COLUMN cor DROP NOT NULL;
ALTER TABLE produtos ALTER COLUMN largura_cm DROP NOT NULL;
ALTER TABLE produtos ALTER COLUMN comprimento_cm DROP NOT NULL;
