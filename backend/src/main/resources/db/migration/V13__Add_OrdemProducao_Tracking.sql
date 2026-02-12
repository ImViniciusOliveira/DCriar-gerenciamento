-- Adiciona rastreamento de Ordem de Produção nas Movimentações de Lote (Matéria-Prima)
ALTER TABLE movimentacoes_estoque_lote
ADD COLUMN ordem_producao_id BIGINT;

ALTER TABLE movimentacoes_estoque_lote
ADD CONSTRAINT fk_mov_lote_ordem
FOREIGN KEY (ordem_producao_id)
REFERENCES ordens_de_producao(id);

-- Adiciona rastreamento de Ordem de Produção nas Movimentações de Produto (Produto Acabado)
ALTER TABLE movimentacoes_estoque_produto
ADD COLUMN ordem_producao_id BIGINT;

ALTER TABLE movimentacoes_estoque_produto
ADD CONSTRAINT fk_mov_prod_ordem
FOREIGN KEY (ordem_producao_id)
REFERENCES ordens_de_producao(id);

-- Adiciona rastreamento de Ordem de Produção nos Lotes de Matéria-Prima (para Retalhos gerados)
ALTER TABLE lotes_materia_prima
ADD COLUMN ordem_producao_origem_id BIGINT;

ALTER TABLE lotes_materia_prima
ADD CONSTRAINT fk_lote_ordem_origem
FOREIGN KEY (ordem_producao_origem_id)
REFERENCES ordens_de_producao(id);
