-- RESPONSABILIDADE: Limpar e popular o banco com um conjunto de dados de desenvolvimento rico e realista.
-- Sendo uma migração REPETÍVEL (R__), o Flyway a executará sempre que o seu conteúdo for alterado.

-- 1. LIMPEZA COMPLETA DAS TABELAS (EM ORDEM DE DEPENDÊNCIA REVERSA)
TRUNCATE TABLE
    itens_venda, vendas, estoques, precos, cortes_realizados, ordem_producao_lotes_consumidos, ordens_de_producao,
    movimentacoes_estoque_produto, movimentacoes_estoque_lote, lotes_materia_prima,
    produtos, tipos_materia_prima, canais_venda
    CASCADE;

-- 2. RESET DAS SEQUÊNCIAS DE ID (MÉTODO LIMPO, SEM OUTPUT NO LOG)
ALTER SEQUENCE tipos_materia_prima_id_seq RESTART WITH 1;
ALTER SEQUENCE lotes_materia_prima_id_seq RESTART WITH 1;
ALTER SEQUENCE movimentacoes_estoque_lote_id_seq RESTART WITH 1;
ALTER SEQUENCE produtos_id_seq RESTART WITH 1;
ALTER SEQUENCE movimentacoes_estoque_produto_id_seq RESTART WITH 1;
ALTER SEQUENCE precos_id_seq RESTART WITH 1;
ALTER SEQUENCE canais_venda_id_seq RESTART WITH 1;
ALTER SEQUENCE estoques_id_seq RESTART WITH 1;
ALTER SEQUENCE vendas_id_seq RESTART WITH 1;
ALTER SEQUENCE itens_venda_id_seq RESTART WITH 1;
ALTER SEQUENCE ordens_de_producao_id_seq RESTART WITH 1;
ALTER SEQUENCE cortes_realizados_id_seq RESTART WITH 1;

-- 3. INSERÇÃO DE DADOS DE DESENVOLVIMENTO

-- ETAPA A: TIPOS DE MATÉRIAS-PRIMAS MAIS REALISTAS
INSERT INTO tipos_materia_prima (nome, unidade_de_consumo) VALUES
    ('Papel Couchê 300g', 'UNIDADE'),                     -- ID 1
    ('Lona Fosca 440g', 'METRO_QUADRADO'),              -- ID 2
    ('Adesivo Vinil Branco', 'METRO_QUADRADO'),         -- ID 3
    ('Adesivo BOPP Transparente', 'UNIDADE'),   -- ID 4
    ('Papel Kraft 180g', 'UNIDADE');                      -- ID 5

-- ETAPA B: LOTES FÍSICOS NO ESTOQUE
INSERT INTO lotes_materia_prima (tipo_materia_prima_id, unidade_de_estoque, atributos, lote_de_origem_id) VALUES
    (1, 'UNIDADE', '{ "formato": "SRA3 (320x450mm)" }', null),        -- Lote ID 1
    (2, 'METRO_QUADRADO', '{ "larguraMm": 1600 }', null),            -- Lote ID 2 (1.6 metros)
    (3, 'METRO_QUADRADO', '{ "larguraMm": 1200 }', null),            -- Lote ID 3 (1.2 metros)
    (4, 'UNIDADE', '{ "formato": "A3 (297x420mm)" }', null),        -- Lote ID 4
    (5, 'UNIDADE', '{ "formato": "A4 (210x297mm)" }', null);         -- Lote ID 5

-- ETAPA C: REGISTRAR AS ENTRADAS DE ESTOQUE DE INSUMOS
INSERT INTO movimentacoes_estoque_lote (lote_id, data, tipo, quantidade, motivo) VALUES
    (1, NOW() - INTERVAL '15 day', 'ENTRADA_COMPRA', 500, 'Nota Fiscal #2024-A1'),
    (2, NOW() - INTERVAL '10 day', 'ENTRADA_COMPRA', 50, 'Nota Fiscal #2024-B2'),
    (3, NOW() - INTERVAL '5 day', 'ENTRADA_COMPRA', 100, 'Nota Fiscal #2024-C3'),
    (4, NOW() - INTERVAL '2 day', 'ENTRADA_COMPRA', 1000, 'Nota Fiscal #2024-D4');

-- ETAPA D: CANAIS DE VENDA
INSERT INTO canais_venda (nome) VALUES
    ('Loja Física'), ('Shopee'), ('Site Próprio'), ('Mercado Livre'), ('Equipe de Vendas');

-- ETAPA E: PRODUTOS ACABADOS DIVERSIFICADOS
INSERT INTO produtos (nome, sku, descricao, cor, unidades_por_produto, ativo, foto_principal_url, tipo_materia_prima_id, largura_cm_unitaria, comprimento_cm_unitario) VALUES
    ('Cartão de Visita Premium', 'CV-PREM-9X5', 'Cartão de visita em papel couchê 300g, laminação fosca.', 'Branco', 100, true, '', 1, 9.0, 5.0),
    ('Banner Comercial 1,20x0,80m', 'BNR-COM-120X80', 'Banner em lona fosca 440g com bastão e corda.', 'Personalizada', 1, true, '', 2, 80.0, 120.0),
    ('Adesivo Redondo 5cm', 'ADSV-RD-5', 'Adesivo em vinil branco para uso geral, corte redondo.', 'Branco', 100, true, '', 3, 5.0, 5.0),
    ('Folder A4 (Dobrado)', 'FLD-A4-OLD', 'Folder promocional antigo. Produto descontinuado.', 'Colorido', 1, false, '', 1, 21.0, 29.7),
    ('Rótulo para Cerveja Long Neck', 'ROT-CERV-LN', 'Rótulo para garrafas, resistente à umidade, em BOPP transparente.', 'Transparente', 50, true, '', 4, 8.0, 7.0),
    ('Adesivo Holográfico (Novo)', 'ADSV-HOLO-10', 'Adesivo com efeito holográfico, corte especial.', 'Holográfico', 100, true, '', 3, 10.0, 10.0),
    ('Tag para Roupas Kraft', 'TAG-KFT-4X9', 'Tag de papel kraft 180g com furo.', 'Pardo', 100, true, '', 5, 4.0, 9.0);

-- ETAPA F: ESTOQUE MESTRE INICIAL (COM VARIAÇÕES)
INSERT INTO movimentacoes_estoque_produto (produto_id, data, tipo, quantidade, motivo) VALUES
    (1, NOW() - INTERVAL '5 day', 'ENTRADA_PRODUCAO', 5000, 'Ordem de Produção #P101'),
    (2, NOW() - INTERVAL '4 day', 'ENTRADA_PRODUCAO', 10, 'Ordem de Produção #P102'),
    (3, NOW() - INTERVAL '3 day', 'ENTRADA_PRODUCAO', 1000, 'Ordem de Produção #P103'),
    (5, NOW() - INTERVAL '2 day', 'ENTRADA_PRODUCAO', 250, 'Ordem de Produção #P104'),
    (7, NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 500, 'Ordem de Produção #P105');

-- ETAPA G: ESTOQUE DISTRIBUÍDO (COM ESTRATÉGIAS DIFERENTES)
INSERT INTO estoques (produto_id, canal_venda_id, quantidade) VALUES
    (1, 3, 4000), (1, 1, 1000), (2, 5, 10), (3, 2, 500), (3, 4, 500), (5, 3, 250), (7, 3, 500);

-- ETAPA H: PREÇOS (PARA PRODUTOS ATIVOS E COM ESTOQUE)
INSERT INTO precos (produto_id, tipo_preco, valor, valor_promocional, promocao_ativa) VALUES
    (1, 'VAREJO', 120.00, 99.90, true), (2, 'VAREJO', 85.00, null, false), (3, 'VAREJO', 45.00, null, false), (5, 'VAREJO', 60.00, null, false), (7, 'VAREJO', 30.00, null, false);

-- ETAPA I: ORDENS DE PRODUÇÃO DE EXEMPLO (Corrigido: sem ID explícito)
INSERT INTO ordens_de_producao (produto_id, quantidade_produzida, modo_calculo, largura_final_cm, comprimento_final_cm, data_criacao, motivo, canal_venda_destino_id, margem_superior_cm, margem_inferior_cm, margem_esquerda_cm, margem_direita_cm) VALUES
    (1, 100, 'AUTOMATICO', 5.2, 500.4, NOW() - INTERVAL '2 day', 'PEDIDO-SHP-101', 2, 0.2, 0.2, 0.1, 0.1),
    (2, 50, 'MANUAL', 10.0, 260.0, NOW() - INTERVAL '1 day', 'PEDIDO-LJA-205', 1, 0.0, 0.0, 0.0, 0.0);

-- ETAPA J: LOTES CONSUMIDOS PELAS ORDENS DE PRODUÇÃO
-- Assumindo que os inserts acima geraram IDs 1 e 2
INSERT INTO ordem_producao_lotes_consumidos (ordem_producao_id, lote_materia_prima_id) VALUES
    (1, 1),
    (2, 2);

-- ETAPA K: CORTES REALIZADOS DE EXEMPLO
-- Assumindo que os inserts acima geraram IDs 1 e 2
INSERT INTO cortes_realizados (ordem_de_producao_id, largura_cm, comprimento_cm, quantidade, tipo, retalho_categoria) VALUES
    (1, 5.0, 5.0, 100, 'PRODUTO', NULL),
    (2, 9.0, 5.0, 50, 'PRODUTO', NULL);

-- ETAPA L: VENDAS DE EXEMPLO
INSERT INTO vendas (data_venda, canal_venda_id, valor_total) VALUES (NOW() - INTERVAL '1 day', 3, 99.90);
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, preco_total) VALUES (1, 1, 1, 99.90, 99.90);

INSERT INTO vendas (data_venda, canal_venda_id, valor_total) VALUES (NOW() - INTERVAL '12 hour', 5, 170.00);
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, preco_total) VALUES (2, 2, 2, 85.00, 170.00);

INSERT INTO vendas (data_venda, canal_venda_id, valor_total) VALUES (NOW(), 2, 75.00);
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, preco_total) VALUES (3, 3, 1, 45.00, 45.00), (3, 7, 1, 30.00, 30.00);
