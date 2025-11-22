-- V10: Torna as colunas de dimensão final em ordens_de_producao nulas.
-- Isso é necessário porque ordens de produção do tipo "consumo direto"
-- não possuem dimensões de corte, e a restrição NOT NULL impedia sua criação.

ALTER TABLE ordens_de_producao ALTER COLUMN largura_final_cm DROP NOT NULL;
ALTER TABLE ordens_de_producao ALTER COLUMN comprimento_final_cm DROP NOT NULL;
