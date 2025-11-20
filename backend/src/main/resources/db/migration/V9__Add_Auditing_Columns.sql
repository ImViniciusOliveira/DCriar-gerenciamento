-- V9: Adiciona colunas de auditoria (data_criacao, data_atualizacao) a várias tabelas.
-- Esta versão é simplificada para um ambiente onde o banco de dados pode ser recriado.

-- Adiciona colunas de auditoria para tabelas que não as possuíam.
ALTER TABLE produtos ADD COLUMN data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
ALTER TABLE produtos ADD COLUMN data_atualizacao TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE tipos_materia_prima ADD COLUMN data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
ALTER TABLE tipos_materia_prima ADD COLUMN data_atualizacao TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE lotes_materia_prima ADD COLUMN data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
ALTER TABLE lotes_materia_prima ADD COLUMN data_atualizacao TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE canais_venda ADD COLUMN data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
ALTER TABLE canais_venda ADD COLUMN data_atualizacao TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE precos ADD COLUMN data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();
ALTER TABLE precos ADD COLUMN data_atualizacao TIMESTAMP WITHOUT TIME ZONE;

-- Padroniza a tabela 'vendas', que já tinha uma coluna de data.
-- Renomeia a coluna existente 'data_venda' para 'data_criacao' para manter o esquema consistente.
ALTER TABLE vendas RENAME COLUMN data_venda TO data_criacao;
-- Adiciona a nova coluna 'data_atualizacao'.
ALTER TABLE vendas ADD COLUMN data_atualizacao TIMESTAMP WITHOUT TIME ZONE;

-- Padroniza a tabela 'ordens_de_producao', que já tinha 'data_criacao'.
-- Apenas adiciona a nova coluna 'data_atualizacao'.
ALTER TABLE ordens_de_producao ADD COLUMN data_atualizacao TIMESTAMP WITHOUT TIME ZONE;
