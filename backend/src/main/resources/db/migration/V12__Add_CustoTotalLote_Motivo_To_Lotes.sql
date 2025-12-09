ALTER TABLE lotes_materia_prima
ADD COLUMN custo_total_lote NUMERIC(10, 4) NOT NULL,
ADD COLUMN motivo VARCHAR(255) NOT NULL;
