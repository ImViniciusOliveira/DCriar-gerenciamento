ALTER TABLE movimentacoes_estoque_produto
    ADD COLUMN produto_nome_snapshot VARCHAR(255) NOT NULL,
    ADD COLUMN produto_sku_snapshot VARCHAR(100) NOT NULL;
