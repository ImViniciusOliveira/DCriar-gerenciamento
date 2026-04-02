UPDATE movimentacoes_estoque_produto
SET tipo = 'ESTORNO_VENDA'
WHERE tipo = 'ENTRADA_ESTORNO';
