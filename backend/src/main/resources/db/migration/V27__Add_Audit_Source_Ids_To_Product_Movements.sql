ALTER TABLE movimentacoes_estoque_produto
    ADD COLUMN ordem_producao_origem_id BIGINT,
    ADD COLUMN venda_origem_id BIGINT;

UPDATE movimentacoes_estoque_produto
SET ordem_producao_origem_id = ordem_producao_id
WHERE ordem_producao_id IS NOT NULL;
