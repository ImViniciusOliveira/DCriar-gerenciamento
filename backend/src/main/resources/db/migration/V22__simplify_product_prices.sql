ALTER TABLE precos
    DROP COLUMN IF EXISTS tipo_preco,
    DROP COLUMN IF EXISTS valor_promocional,
    DROP COLUMN IF EXISTS promocao_ativa;

ALTER TABLE precos
    ADD CONSTRAINT uk_precos_produto UNIQUE (produto_id);
