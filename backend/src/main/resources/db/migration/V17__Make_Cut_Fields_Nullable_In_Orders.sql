-- RESPONSABILIDADE: Tornar as colunas específicas de "Corte" nulas na tabela de ordens de produção.
-- MOTIVO: Permitir que ordens de "Consumo" sejam criadas sem a necessidade de preencher
-- campos que não se aplicam a elas, como modo de cálculo e dimensões finais.

ALTER TABLE ordens_de_producao
    ALTER COLUMN modo_calculo DROP NOT NULL,
    ALTER COLUMN largura_final_cm DROP NOT NULL,
    ALTER COLUMN comprimento_final_cm DROP NOT NULL;
