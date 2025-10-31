-- 1. Renomeia a tabela principal de 'ordens_de_corte' para 'ordens_de_producao'
ALTER TABLE ordens_de_corte RENAME TO ordens_de_producao;

-- 2. Remove a antiga coluna de lote único. O comando DROP COLUMN remove automaticamente as constraints associadas.
ALTER TABLE ordens_de_producao DROP COLUMN lote_principal_id;

-- 3. Cria a nova tabela de junção para suportar múltiplos lotes consumidos por ordem
CREATE TABLE ordem_producao_lotes_consumidos (
    ordem_producao_id BIGINT NOT NULL,
    lote_materia_prima_id BIGINT NOT NULL,
    PRIMARY KEY (ordem_producao_id, lote_materia_prima_id),
    CONSTRAINT fk_op_lotes_ordem FOREIGN KEY (ordem_producao_id) REFERENCES ordens_de_producao(id) ON DELETE CASCADE,
    CONSTRAINT fk_op_lotes_lote FOREIGN KEY (lote_materia_prima_id) REFERENCES lotes_materia_prima(id)
);

-- 4. Atualiza a tabela 'cortes_realizados' para apontar para a nova tabela 'ordens_de_producao'
ALTER TABLE cortes_realizados RENAME COLUMN ordem_de_corte_id TO ordem_de_producao_id;

-- 5. Renomeia os objetos associados para manter a consistência
ALTER INDEX ordens_de_corte_pkey RENAME TO ordens_de_producao_pkey;
ALTER SEQUENCE ordens_de_corte_id_seq RENAME TO ordens_de_producao_id_seq;
