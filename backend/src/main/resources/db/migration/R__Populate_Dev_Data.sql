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
INSERT INTO tipos_materia_prima (nome, unidade_de_consumo, data_criacao, data_atualizacao) VALUES
    ('Papel Couchê 300g', 'UNIDADE', NOW(), NOW()),
    ('Lona Fosca 440g', 'METRO_QUADRADO', NOW(), NOW()),
    ('Adesivo Vinil Branco', 'METRO_QUADRADO', NOW(), NOW()),
    ('Adesivo BOPP Transparente', 'UNIDADE', NOW(), NOW()),
    ('Papel Kraft 180g', 'UNIDADE', NOW(), NOW()),
    ('Tinta Eco-Solvente Preta', 'LITRO', NOW(), NOW()),
    ('Fita Dupla Face 25mm', 'METRO_LINEAR', NOW(), NOW()),
    ('Ilhós de Latão #0', 'UNIDADE', NOW(), NOW());

-- ETAPA B: LOTES FÍSICOS NO ESTOQUE
-- Adicionados custo_total_lote e motivo para cada lote
INSERT INTO lotes_materia_prima (tipo_materia_prima_id, unidade_de_estoque, custo_total_lote, motivo, atributos, lote_de_origem_id, data_criacao, data_atualizacao) VALUES
    (1, 'UNIDADE', 150.00, 'Compra NF-1001', '{ "formato": "SRA3 (320x450mm)" }', null, NOW(), NOW()),
    (2, 'METRO_QUADRADO', 250.00, 'Compra NF-1002', '{ "larguraMm": 1600 }', null, NOW(), NOW()),
    (3, 'METRO_QUADRADO', 120.00, 'Compra NF-1003', '{ "larguraMm": 1200 }', null, NOW(), NOW()),
    (4, 'UNIDADE', 80.00, 'Compra NF-1004', '{ "formato": "A3 (297x420mm)" }', null, NOW(), NOW()),
    (5, 'UNIDADE', 50.00, 'Compra NF-1005', '{ "formato": "A4 (210x297mm)" }', null, NOW(), NOW()),
    (6, 'LITRO', 300.00, 'Compra NF-1006', '{ "fornecedor": "InkMaster" }', null, NOW(), NOW()),
    (7, 'METRO_LINEAR', 75.00, 'Compra NF-1007', '{ "metragem_total_m": 50 }', null, NOW(), NOW()),
    (8, 'UNIDADE', 200.00, 'Compra NF-1008', '{ "quantidade_caixa": 1000 }', null, NOW(), NOW());

-- ETAPA C: REGISTRAR AS ENTRADAS DE ESTOQUE DE INSUMOS
INSERT INTO movimentacoes_estoque_lote (lote_id, data, tipo, quantidade, motivo) VALUES
    (1, NOW() - INTERVAL '15 day', 'ENTRADA_COMPRA', 500, 'Nota Fiscal #2024-A1'),
    (2, NOW() - INTERVAL '10 day', 'ENTRADA_COMPRA', 50, 'Nota Fiscal #2024-B2'),
    (3, NOW() - INTERVAL '5 day', 'ENTRADA_COMPRA', 100, 'Nota Fiscal #2024-C3'),
    (4, NOW() - INTERVAL '2 day', 'ENTRADA_COMPRA', 1000, 'Nota Fiscal #2024-D4'),
    (6, NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 5, 'Nota Fiscal #2024-E5'),
    (7, NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 50, 'Nota Fiscal #2024-F6'),
    (8, NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 1000, 'Nota Fiscal #2024-G7');

-- ETAPA D: CANAIS DE VENDA
INSERT INTO canais_venda (nome, data_criacao, data_atualizacao) VALUES
    ('Loja Física', NOW(), NOW()),
    ('Shopee', NOW(), NOW()),
    ('Site Próprio', NOW(), NOW()),
    ('Mercado Livre', NOW(), NOW()),
    ('Equipe de Vendas', NOW(), NOW());

-- ETAPA E: PRODUTOS ACABADOS DIVERSIFICADOS
INSERT INTO produtos (tipo_produto, nome, sku, descricao, cor, unidades_por_produto, ativo, foto_principal_url, tipo_materia_prima_id, largura_cm, comprimento_cm, data_criacao, data_atualizacao, codigo_fabricante, especificacoes) VALUES
    ('CORTE', 'Cartão de Visita Premium', 'CV-PREM-9X5', 'Cartão de visita em papel couchê 300g, laminação fosca.', 'Branco', 100, true, '', 1, 9.0, 5.0, NOW(), NOW(), null, null),
    ('CORTE', 'Banner Comercial 1,20x0,80m', 'BNR-COM-120X80', 'Banner em lona fosca 440g com bastão e corda.', 'Personalizada', 1, true, '', 2, 80.0, 120.0, NOW(), NOW(), null, null),
    ('CORTE', 'Adesivo Redondo 5cm', 'ADSV-RD-5', 'Adesivo em vinil branco para uso geral, corte redondo.', 'Branco', 100, true, '', 3, 5.0, 5.0, NOW(), NOW(), null, null),
    ('CORTE', 'Folder A4 (Dobrado)', 'FLD-A4-OLD', 'Folder promocional antigo. Produto descontinuado.', 'Colorido', 1, false, '', 1, 21.0, 29.7, NOW(), NOW(), null, null),
    ('CORTE', 'Rótulo para Cerveja Long Neck', 'ROT-CERV-LN', 'Rótulo para garrafas, resistente à umidade, em BOPP transparente.', 'Transparente', 50, true, '', 4, 8.0, 7.0, NOW(), NOW(), null, null),
    ('CORTE', 'Adesivo Holográfico (Novo)', 'ADSV-HOLO-10', 'Adesivo com efeito holográfico, corte especial.', 'Holográfico', 100, true, '', 3, 10.0, 10.0, NOW(), NOW(), null, null),
    ('CORTE', 'Tag para Roupas Kraft', 'TAG-KFT-4X9', 'Tag de papel kraft 180g com furo.', 'Pardo', 100, true, '', 5, 4.0, 9.0, NOW(), NOW(), null, null),
    ('CONSUMO_DIRETO', 'Tinta Preta Eco-Solvente (Litro)', 'TIN-PRE-ES-1L', 'Tinta preta para impressoras eco-solvente, garrafa de 1 litro.', null, 1, true, '', 6, null, null, NOW(), NOW(), 'INK-BLK-ES-1L', '{"tipo_tinta": "Eco-Solvente", "cor_pantone": "Black C", "volume_ml": 1000}'),
    ('CONSUMO_DIRETO', 'Rolo de Fita Dupla Face 25mm', 'FITA-DF-25MM', 'Rolo de fita dupla face de alta aderência com 25mm de largura e 50m de comprimento.', null, 1, true, '', 7, null, null, NOW(), NOW(), '3M-VHB-25', '{"largura_mm": 25, "metragem_m": 50, "adesao": "Alta"}'),
    ('CONSUMO_DIRETO', 'Pacote de Ilhós para Banner', 'ILHOS-BNR-100', 'Pacote com 100 unidades de ilhós de latão nº 0 para acabamento de banners.', null, 100, true, '', 8, null, null, NOW(), NOW(), 'ILHOS-LT-0', '{"diametro_mm": 10, "material": "Latão"}');

-- ETAPA F: ESTOQUE MESTRE INICIAL (COM VARIAÇÕES)
INSERT INTO movimentacoes_estoque_produto (produto_id, data, tipo, quantidade, motivo) VALUES
    (1, NOW() - INTERVAL '5 day', 'ENTRADA_PRODUCAO', 5000, 'Ordem de Produção #P101'),
    (2, NOW() - INTERVAL '4 day', 'ENTRADA_PRODUCAO', 10, 'Ordem de Produção #P102'),
    (3, NOW() - INTERVAL '3 day', 'ENTRADA_PRODUCAO', 1000, 'Ordem de Produção #P103'),
    (5, NOW() - INTERVAL '2 day', 'ENTRADA_PRODUCAO', 250, 'Ordem de Produção #P104'),
    (7, NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 500, 'Ordem de Produção #P105'),
    (8, NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 10, 'Entrada de estoque inicial'),
    (9, NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 5, 'Entrada de estoque inicial'),
    (10, NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 20, 'Entrada de estoque inicial');

-- ETAPA G: ESTOQUE DISTRIBUÍDO (COM ESTRATÉGIAS DIFERENTES)
INSERT INTO estoques (produto_id, canal_venda_id, quantidade) VALUES
    (1, 3, 4000), (1, 1, 1000), (2, 5, 10), (3, 2, 500), (3, 4, 500), (5, 3, 250), (7, 3, 500), (8, 1, 10), (9, 1, 5), (10, 1, 20);

-- ETAPA H: PREÇOS (PARA PRODUTOS ATIVOS E COM ESTOQUE)
INSERT INTO precos (produto_id, tipo_preco, valor, valor_promocional, promocao_ativa, data_criacao, data_atualizacao) VALUES
    (1, 'VAREJO', 120.00, 99.90, true, NOW(), NOW()),
    (2, 'VAREJO', 85.00, null, false, NOW(), NOW()),
    (3, 'VAREJO', 45.00, null, false, NOW(), NOW()),
    (4, 'VAREJO', 15.00, null, false, NOW(), NOW()),
    (5, 'VAREJO', 60.00, null, false, NOW(), NOW()),
    (6, 'VAREJO', 55.00, null, false, NOW(), NOW()),
    (7, 'VAREJO', 30.00, null, false, NOW(), NOW()),
    (8, 'VAREJO', 350.00, null, false, NOW(), NOW()),
    (9, 'VAREJO', 75.00, null, false, NOW(), NOW()),
    (10, 'VAREJO', 50.00, null, false, NOW(), NOW());

-- ETAPA I: ORDENS DE PRODUÇÃO DE EXEMPLO
INSERT INTO ordens_de_producao (produto_id, quantidade_produzida, modo_calculo, largura_final_cm, comprimento_final_cm, data_criacao, data_atualizacao, motivo, canal_venda_destino_id, margem_superior_cm, margem_inferior_cm, margem_esquerda_cm, margem_direita_cm) VALUES
    (1, 100, 'AUTOMATICO', 5.2, 500.4, NOW() - INTERVAL '2 day', NOW() - INTERVAL '2 day', 'PEDIDO-SHP-101', 2, 0.2, 0.2, 0.1, 0.1),
    (2, 50, 'MANUAL', 10.0, 260.0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 'PEDIDO-LJA-205', 1, 0.0, 0.0, 0.0, 0.0);

-- ETAPA J: LOTES CONSUMIDOS PELAS ORDENS DE PRODUÇÃO
INSERT INTO ordem_producao_lotes_consumidos (ordem_producao_id, lote_materia_prima_id) VALUES
    (1, 1),
    (2, 2);

-- ETAPA K: CORTES REALIZADOS DE EXEMPLO
INSERT INTO cortes_realizados (ordem_de_producao_id, largura_cm, comprimento_cm, quantidade, tipo, retalho_categoria) VALUES
    (1, 5.0, 5.0, 100, 'PRODUTO', NULL),
    (2, 9.0, 5.0, 50, 'PRODUTO', NULL);

-- ETAPA L: VENDAS DE EXEMPLO
INSERT INTO vendas (data_criacao, data_atualizacao, canal_venda_id, valor_total) VALUES (NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 3, 99.90);
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, preco_total) VALUES (1, 1, 1, 99.90, 99.90);

INSERT INTO vendas (data_criacao, data_atualizacao, canal_venda_id, valor_total) VALUES (NOW() - INTERVAL '12 hour', NOW() - INTERVAL '12 hour', 5, 170.00);
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, preco_total) VALUES (2, 2, 2, 85.00, 170.00);

INSERT INTO vendas (data_criacao, data_atualizacao, canal_venda_id, valor_total) VALUES (NOW(), NOW(), 2, 75.00);
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, preco_total) VALUES (3, 3, 1, 45.00, 45.00), (3, 7, 1, 30.00, 30.00);
