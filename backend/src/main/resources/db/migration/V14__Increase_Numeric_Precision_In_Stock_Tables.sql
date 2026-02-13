-- RESPONSABILIDADE: Aumentar a precisão de colunas numéricas para suportar valores maiores e evitar erros de overflow.

-- Corrige a tabela de lotes para aceitar custos totais maiores.
-- O limite anterior de NUMERIC(10, 4) era muito baixo (máximo de 999,999.9999).
ALTER TABLE lotes_materia_prima
ALTER COLUMN custo_total_lote TYPE NUMERIC(19, 4);

-- Aumenta a precisão do custo unitário para evitar problemas de arredondamento
-- com materiais de baixo custo por unidade de consumo.
-- Esta alteração é preventiva e melhora a acuracidade do sistema.
ALTER TABLE movimentacoes_estoque_lote
ALTER COLUMN custo_por_unidade_base TYPE NUMERIC(19, 8);
