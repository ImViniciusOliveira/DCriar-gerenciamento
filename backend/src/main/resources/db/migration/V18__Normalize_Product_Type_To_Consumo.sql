UPDATE produtos
SET tipo_produto = 'CONSUMO'
WHERE tipo_produto IS NOT NULL
  AND tipo_produto <> 'CORTE'
  AND tipo_produto <> 'CONSUMO';
