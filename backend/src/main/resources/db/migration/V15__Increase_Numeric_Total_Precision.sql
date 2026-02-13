-- RESPONSABILIDADE: Aumentar a precisão total das colunas numéricas para suportar valores monetários e de quantidade muito grandes.

-- Aumenta a capacidade do Custo Total na tabela de Lotes para 19 dígitos inteiros e 8 decimais.
ALTER TABLE lotes_materia_prima
ALTER COLUMN custo_total_lote TYPE NUMERIC(27, 8);

-- Aumenta a capacidade da Quantidade na tabela de Movimentações para 19 dígitos inteiros e 8 decimais.
ALTER TABLE movimentacoes_estoque_lote
ALTER COLUMN quantidade TYPE NUMERIC(27, 8);

-- Aumenta a capacidade do Custo por Unidade Base para 19 dígitos inteiros e 8 decimais.
ALTER TABLE movimentacoes_estoque_lote
ALTER COLUMN custo_por_unidade_base TYPE NUMERIC(27, 8);
