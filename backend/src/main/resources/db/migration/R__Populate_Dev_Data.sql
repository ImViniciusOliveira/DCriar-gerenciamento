-- RESPONSABILIDADE: Limpar e popular o banco com um conjunto de dados de desenvolvimento rico e realista.
-- Sendo uma migração REPETÍVEL (R__), o Flyway a executará sempre que o seu conteúdo for alterado.
-- Esta abordagem usa subqueries para buscar chaves estrangeiras, eliminando a necessidade de IDs manuais e
-- resolvendo permanentemente problemas de sincronização de sequência do PostgreSQL.

-- ETAPA 1: LIMPEZA COMPLETA DAS TABELAS
-- A limpeza é feita em ordem de dependência reversa para respeitar as constraints de chave estrangeira.
-- O TRUNCATE zera as tabelas e o CASCADE garante que as dependências sejam resolvidas.
TRUNCATE TABLE
    itens_venda, vendas, estoques, precos, cortes_realizados, ordem_producao_lotes_consumidos,
    movimentacoes_estoque_produto, movimentacoes_estoque_lote, lotes_materia_prima,
    ordens_de_producao, produtos, tipos_materia_prima, canais_venda
    CASCADE;

-- ETAPA 2: INSERÇÃO DE DADOS MESTRES (Entidades que não dependem de outras)
-- Estas são as tabelas base do sistema.

-- Inserção de Tipos de Matérias-Primas
INSERT INTO tipos_materia_prima (nome, unidade_de_consumo, data_criacao, data_atualizacao) VALUES
    ('Papel Couchê 300g', 'METRO_QUADRADO', NOW(), NOW()),
    ('Lona Fosca 440g', 'METRO_QUADRADO', NOW(), NOW()),
    ('Adesivo Vinil Branco', 'METRO_QUADRADO', NOW(), NOW()),
    ('Adesivo BOPP Transparente', 'METRO_QUADRADO', NOW(), NOW()),
    ('Papel Kraft 180g', 'METRO_QUADRADO', NOW(), NOW()),
    ('Tinta Eco-Solvente Preta', 'LITRO', NOW(), NOW()),
    ('Fita Dupla Face 25mm', 'METRO_LINEAR', NOW(), NOW()),
    ('Ilhós de Latão #0', 'UNIDADE', NOW(), NOW()),
    ('Resina Epóxi Transparente', 'QUILOGRAMA', NOW(), NOW()),
    ('Pó Adesivo Termocolante', 'GRAMA', NOW(), NOW()),
    ('Verniz UV Brilhante', 'MILILITRO', NOW(), NOW()),
    ('Papel Seda Branco A4', 'FOLHA', NOW(), NOW());

-- Inserção de Canais de Venda
INSERT INTO canais_venda (nome, data_criacao, data_atualizacao) VALUES
    ('Loja Física', NOW(), NOW()),
    ('Shopee', NOW(), NOW()),
    ('Site Próprio', NOW(), NOW()),
    ('Mercado Livre', NOW(), NOW()),
    ('Equipe de Vendas', NOW(), NOW());

-- ETAPA 3: INSERÇÃO DE DADOS DEPENDENTES (Nível 1)
-- Estas tabelas dependem dos dados mestres inseridos na Etapa 2.

-- Inserção de Lotes de Matéria-Prima (dependem de Tipos de Matérias-Primas)
INSERT INTO lotes_materia_prima (tipo_materia_prima_id, unidade_de_estoque, custo_total_lote, motivo, atributos, data_criacao, data_atualizacao) VALUES
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Couchê 300g'), 'METRO_QUADRADO', 150.00, 'Compra NF-1001', '{ "larguraMm": 320, "comprimentoMm": 450 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Lona Fosca 440g'), 'METRO_QUADRADO', 250.00, 'Compra NF-1002', '{ "larguraMm": 1600 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo Vinil Branco'), 'METRO_QUADRADO', 120.00, 'Compra NF-1003', '{ "larguraMm": 1200 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo BOPP Transparente'), 'METRO_QUADRADO', 80.00, 'Compra NF-1004', '{ "larguraMm": 297, "comprimentoMm": 420 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Kraft 180g'), 'METRO_QUADRADO', 50.00, 'Compra NF-1005', '{ "larguraMm": 210, "comprimentoMm": 297 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Tinta Eco-Solvente Preta'), 'LITRO', 300.00, 'Compra NF-1006', '{ "fornecedor": "InkMaster" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Fita Dupla Face 25mm'), 'METRO_LINEAR', 75.00, 'Compra NF-1007', '{ "larguraMm": 25, "metragem_m": 50 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Ilhós de Latão #0'), 'UNIDADE', 200.00, 'Compra NF-1008', '{ "quantidade_caixa": 1000 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Resina Epóxi Transparente'), 'QUILOGRAMA', 1800.00, 'Compra NF-1009', '{ "fornecedor": "Quimicolor", "lote_fabricante": "EPX-2309" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Pó Adesivo Termocolante'), 'GRAMA', 950.00, 'Compra NF-1010', '{ "fornecedor": "PrintBond", "malha": "fina" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Verniz UV Brilhante'), 'MILILITRO', 700.00, 'Compra NF-1011', '{ "fornecedor": "UV Coatings", "acabamento": "brilho" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Seda Branco A4'), 'FOLHA', 250.00, 'Compra NF-1012', '{ "gramatura_gm2": 18, "cor": "branco" }', NOW(), NOW());

-- Inserção de Produtos Acabados (dependem de Tipos de Matérias-Primas)
INSERT INTO produtos (tipo_produto, nome, sku, descricao, cor, unidades_por_produto, ativo, foto_principal_url, tipo_materia_prima_id, largura_cm, comprimento_cm, data_criacao, data_atualizacao, codigo_fabricante, especificacoes) VALUES
    ('CORTE', 'Cartão de Visita Premium', 'CV-PREM-9X5', 'Cartão de visita em papel couchê 300g, laminação fosca.', 'Branco', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Couchê 300g'), 9.0, 5.0, NOW(), NOW(), null, null),
    ('CORTE', 'Banner Comercial 1,20x0,80m', 'BNR-COM-120X80', 'Banner em lona fosca 440g com bastão e corda.', 'Personalizada', 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Lona Fosca 440g'), 80.0, 120.0, NOW(), NOW(), null, null),
    ('CORTE', 'Adesivo Redondo 5cm', 'ADSV-RD-5', 'Adesivo em vinil branco para uso geral, corte redondo.', 'Branco', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo Vinil Branco'), 5.0, 5.0, NOW(), NOW(), null, null),
    ('CORTE', 'Folder A4 Dobrado', 'FLD-A4-DOB', 'Folder promocional em papel couchê 300g com dobra central.', 'Colorido', 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Couchê 300g'), 21.0, 29.7, NOW(), NOW(), null, null),
    ('CORTE', 'Rótulo para Cerveja Long Neck', 'ROT-CERV-LN', 'Rótulo para garrafas, resistente à umidade, em BOPP transparente.', 'Transparente', 50, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo BOPP Transparente'), 8.0, 7.0, NOW(), NOW(), null, null),
    ('CORTE', 'Adesivo Holográfico 10x10cm', 'ADSV-HOLO-10', 'Adesivo com acabamento holográfico para brindes e embalagens.', 'Holográfico', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo Vinil Branco'), 10.0, 10.0, NOW(), NOW(), null, null),
    ('CORTE', 'Tag Kraft para Roupas 4x9cm', 'TAG-KFT-4X9', 'Tag em papel kraft 180g com furo para aplicação em peças de vestuário.', 'Pardo', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Kraft 180g'), 4.0, 9.0, NOW(), NOW(), null, null),
    ('CONSUMO', 'Tinta Eco-Solvente Preta', 'TIN-PRE-ES-1L', 'Tinta preta para impressoras eco-solvente, frasco com 1 litro.', null, 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Tinta Eco-Solvente Preta'), null, null, NOW(), NOW(), 'INK-BLK-ES-1L', '{"tipo_tinta": "Eco-Solvente", "cor_pantone": "Black C", "volume_ml": 1000}'),
    ('CORTE', 'Fita Dupla Face 25mm x 50m', 'FITA-DF-25MM', 'Rolo de fita dupla face de alta aderência com 25 mm de largura e 50 metros de comprimento.', 'Transparente', 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Fita Dupla Face 25mm'), 2.5, 5000.0, NOW(), NOW(), null, null),
    ('CONSUMO', 'Pacote de Ilhós Nº 0', 'ILHOS-BNR-100', 'Pacote com 100 unidades de ilhós de latão número 0 para acabamento de banners.', null, 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Ilhós de Latão #0'), null, null, NOW(), NOW(), 'ILHOS-LT-0', '{"diametro_mm": 10, "material": "Latão"}'),
    ('CONSUMO', 'Kit de Resina Epóxi', 'RES-EPX-2KG', 'Kit de resina epóxi transparente para artesanato e encapsulamento, com 2 kg.', null, 2, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Resina Epóxi Transparente'), null, null, NOW(), NOW(), 'EPX-KIT-2KG', '{"acabamento": "transparente", "uso": "artesanato"}'),
    ('CONSUMO', 'Refil de Pó Adesivo', 'PO-ADT-500G', 'Refil de pó adesivo termocolante para DTF, embalagem com 500 gramas.', null, 500, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Pó Adesivo Termocolante'), null, null, NOW(), NOW(), 'DTF-PO-500', '{"malha": "fina", "uso": "transfer"}'),
    ('CONSUMO', 'Frasco de Verniz UV', 'VERN-UV-250', 'Frasco de verniz UV brilhante para acabamento gráfico, com 250 ml.', null, 250, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Verniz UV Brilhante'), null, null, NOW(), NOW(), 'UV-BRILHO-250', '{"acabamento": "brilhante", "cura": "UV"}'),
    ('CONSUMO', 'Pacote de Papel Seda A4', 'PAP-SEDA-A4-100', 'Pacote de papel seda branco A4 com 100 folhas para proteção e acabamento.', null, 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Seda Branco A4'), null, null, NOW(), NOW(), 'SEDA-A4-100', '{"cor": "branco", "gramatura_gm2": 18}');

-- ETAPA 4: INSERÇÃO DE DADOS DEPENDENTES (Nível 2)
-- Estas tabelas dependem dos dados inseridos nas Etapas 2 e 3.

-- Inserção de Movimentações de Estoque de Lote (dependem de Lotes)
-- Garante que cada lote de compra tenha um saldo inicial.
INSERT INTO movimentacoes_estoque_lote (lote_id, data, tipo, quantidade, motivo) VALUES
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1001'), NOW() - INTERVAL '15 day', 'ENTRADA_COMPRA', 500, 'Nota Fiscal #2024-A1'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1002'), NOW() - INTERVAL '10 day', 'ENTRADA_COMPRA', 50, 'Nota Fiscal #2024-B2'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1003'), NOW() - INTERVAL '5 day', 'ENTRADA_COMPRA', 100, 'Nota Fiscal #2024-C3'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1004'), NOW() - INTERVAL '2 day', 'ENTRADA_COMPRA', 1000, 'Nota Fiscal #2024-D4'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1005'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 500, 'Nota Fiscal #2024-H8'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1006'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 5, 'Nota Fiscal #2024-E5'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1007'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 50, 'Nota Fiscal #2024-F6'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1008'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 1000, 'Nota Fiscal #2024-G7'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1009'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 30, 'Nota Fiscal #2024-I9'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1010'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 20000, 'Nota Fiscal #2024-J10'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1011'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 10000, 'Nota Fiscal #2024-K11'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1012'), NOW() - INTERVAL '1 day', 'ENTRADA_COMPRA', 5000, 'Nota Fiscal #2024-L12');

-- Inserção de Estoque Distribuído (dependem de Produtos e Canais de Venda)
INSERT INTO estoques (produto_id, canal_venda_id, quantidade) VALUES
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 4000),
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 1000),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), (SELECT id FROM canais_venda WHERE nome = 'Equipe de Vendas'), 10),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), (SELECT id FROM canais_venda WHERE nome = 'Shopee'), 500),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), (SELECT id FROM canais_venda WHERE nome = 'Mercado Livre'), 500),
    ((SELECT id FROM produtos WHERE sku = 'ROT-CERV-LN'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 250),
    ((SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 500),
    ((SELECT id FROM produtos WHERE sku = 'TIN-PRE-ES-1L'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 10),
    ((SELECT id FROM produtos WHERE sku = 'FITA-DF-25MM'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 5),
    ((SELECT id FROM produtos WHERE sku = 'ILHOS-BNR-100'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 20),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 8),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 15),
    ((SELECT id FROM produtos WHERE sku = 'VERN-UV-250'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 12),
    ((SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 20);

-- Inserção de Preços (dependem de Produtos)
INSERT INTO precos (produto_id, tipo_preco, valor, valor_promocional, promocao_ativa, data_criacao, data_atualizacao) VALUES
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), 'VAREJO', 120.00, 99.90, true, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), 'VAREJO', 85.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), 'VAREJO', 45.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'FLD-A4-DOB'), 'VAREJO', 15.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ROT-CERV-LN'), 'VAREJO', 60.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-HOLO-10'), 'VAREJO', 55.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'), 'VAREJO', 30.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'TIN-PRE-ES-1L'), 'VAREJO', 350.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'FITA-DF-25MM'), 'VAREJO', 75.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ILHOS-BNR-100'), 'VAREJO', 50.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), 'VAREJO', 189.90, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), 'VAREJO', 79.90, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'VERN-UV-250'), 'VAREJO', 45.00, null, false, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'), 'VAREJO', 35.00, null, false, NOW(), NOW());

-- ETAPA 5: INSERÇÃO DE DADOS DE PRODUÇÃO E VENDAS (Nível 3 - dependem de tudo acima)
-- Estas são as tabelas transacionais que representam as operações do dia a dia.

-- Inserção de Ordens de Produção (dependem de Produtos e Canais de Venda)
INSERT INTO ordens_de_producao (produto_id, quantidade_produzida, modo_calculo, largura_final_cm, comprimento_final_cm, data_criacao, data_atualizacao, motivo, canal_venda_destino_id, margem_superior_cm, margem_inferior_cm, margem_esquerda_cm, margem_direita_cm) VALUES
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), 100, 'AUTOMATICO', 5.2, 500.4, NOW() - INTERVAL '2 day', NOW() - INTERVAL '2 day', 'PEDIDO-SHP-101', (SELECT id FROM canais_venda WHERE nome = 'Shopee'), 0.2, 0.2, 0.1, 0.1),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), 50, 'MANUAL', 10.0, 260.0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 'PEDIDO-LJA-205', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 0.0, 0.0, 0.0, 0.0),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), 50, 'MANUAL', 100.0, 10.0, NOW() - INTERVAL '3 hour', NOW() - INTERVAL '3 hour', 'Teste Estorno Válido', null, 0, 0, 0, 0),
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), 1000, 'MANUAL', 50.0, 100.0, NOW() - INTERVAL '2 hour', NOW() - INTERVAL '2 hour', 'Teste Bloqueio por Venda', null, 0, 0, 0, 0),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), 20, 'MANUAL', 80.0, 2400.0, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour', 'Teste Bloqueio por Retalho - Geradora', null, 0, 0, 0, 0),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), 10, 'MANUAL', 20.0, 50.0, NOW() - INTERVAL '30 minute', NOW() - INTERVAL '30 minute', 'Teste Bloqueio por Retalho - Consumidora', null, 0, 0, 0, 0),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), 8, null, null, null, NOW() - INTERVAL '20 hour', NOW() - INTERVAL '20 hour', 'LOTE-INT-RESINA-201', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), null, null, null, null),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), 15, null, null, null, NOW() - INTERVAL '18 hour', NOW() - INTERVAL '18 hour', 'REPOSICAO-DTF-305', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), null, null, null, null),
    ((SELECT id FROM produtos WHERE sku = 'VERN-UV-250'), 12, null, null, null, NOW() - INTERVAL '16 hour', NOW() - INTERVAL '16 hour', 'REPOSICAO-UV-410', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), null, null, null, null),
    ((SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'), 20, null, null, null, NOW() - INTERVAL '14 hour', NOW() - INTERVAL '14 hour', 'ESTOQUE-PAPEL-SEDA-112', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), null, null, null, null);

-- Inserção de Lotes de Retalho (dependem de Lotes de Compra e Ordens de Produção)
-- É crucial que esta etapa venha DEPOIS da criação das Ordens de Produção.
INSERT INTO lotes_materia_prima (tipo_materia_prima_id, unidade_de_estoque, custo_total_lote, motivo, atributos, lote_de_origem_id, ordem_producao_origem_id, data_criacao, data_atualizacao) VALUES
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo Vinil Branco'), 'METRO_QUADRADO', 0.00, 'Retalho da Ordem #3', '{ "larguraMm": 200 }', (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1003'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Estorno Válido'), NOW() - INTERVAL '3 hour', NOW() - INTERVAL '3 hour'),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Lona Fosca 440g'), 'METRO_QUADRADO', 0.00, 'Retalho da Ordem #5', '{ "larguraMm": 300 }', (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1002'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Geradora'), NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour');

-- Inserção de Vendas (dependem de Canais de Venda)
INSERT INTO vendas (data_criacao, data_atualizacao, canal_venda_id, valor_total) VALUES
    (NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 99.90),
    (NOW() - INTERVAL '12 hour', NOW() - INTERVAL '12 hour', (SELECT id FROM canais_venda WHERE nome = 'Equipe de Vendas'), 170.00),
    (NOW(), NOW(), (SELECT id FROM canais_venda WHERE nome = 'Shopee'), 75.00),
    (NOW() - INTERVAL '10 hour', NOW() - INTERVAL '10 hour', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 189.90),
    (NOW() - INTERVAL '8 hour', NOW() - INTERVAL '8 hour', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 159.80);

-- ETAPA 6: INSERÇÃO DE DADOS DE RELACIONAMENTO E HISTÓRICO (Nível Final)
-- Estas são as tabelas de junção e logs que dependem de todas as outras entidades.

-- Vínculos de Consumo (Ordem -> Lote)
INSERT INTO ordem_producao_lotes_consumidos (ordem_producao_id, lote_materia_prima_id) VALUES
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1001')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1002')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Estorno Válido'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1003')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Venda'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1001')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Geradora'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1002')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Consumidora'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Retalho da Ordem #5')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1009')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1010')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1011')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1012'));

-- Cortes Realizados (dependem de Ordens de Produção)
INSERT INTO cortes_realizados (ordem_de_producao_id, largura_cm, comprimento_cm, quantidade, tipo, retalho_categoria) VALUES
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101'), 5.0, 5.0, 100, 'PRODUTO', NULL),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205'), 9.0, 5.0, 50, 'PRODUTO', NULL),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Estorno Válido'), 5.0, 5.0, 50, 'PRODUTO', NULL),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Venda'), 9.0, 5.0, 1000, 'PRODUTO', NULL),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Geradora'), 80.0, 120.0, 20, 'PRODUTO', NULL),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Consumidora'), 5.0, 5.0, 10, 'PRODUTO', NULL);

-- Itens de Venda (dependem de Vendas e Produtos)
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_unitario, preco_total) VALUES
    ((SELECT id FROM vendas WHERE valor_total = 99.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio')), (SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), 1, 99.90, 99.90),
    ((SELECT id FROM vendas WHERE valor_total = 170.00 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Equipe de Vendas')), (SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), 2, 85.00, 170.00),
    ((SELECT id FROM vendas WHERE valor_total = 75.00 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Shopee')), (SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), 1, 45.00, 45.00),
    ((SELECT id FROM vendas WHERE valor_total = 75.00 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Shopee')), (SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'), 1, 30.00, 30.00),
    ((SELECT id FROM vendas WHERE valor_total = 189.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Loja Física')), (SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), 1, 189.90, 189.90),
    ((SELECT id FROM vendas WHERE valor_total = 159.80 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio')), (SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), 2, 79.90, 159.80);

-- Histórico de Movimentações de Estoque de Produto (dependem de Produtos e Ordens de Produção)
INSERT INTO movimentacoes_estoque_produto (produto_id, data, tipo, quantidade, motivo, ordem_producao_id) VALUES
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), NOW() - INTERVAL '5 day', 'ENTRADA_PRODUCAO', 5000, 'Ordem de Produção #P101', (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101')),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), NOW() - INTERVAL '4 day', 'ENTRADA_PRODUCAO', 10, 'Ordem de Produção #P102', (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205')),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), NOW() - INTERVAL '3 day', 'ENTRADA_PRODUCAO', 1000, 'Ordem de Produção #P103', null),
    ((SELECT id FROM produtos WHERE sku = 'ROT-CERV-LN'), NOW() - INTERVAL '2 day', 'ENTRADA_PRODUCAO', 250, 'Ordem de Produção #P104', null),
    ((SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'), NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 500, 'Ordem de Produção #P105', null),
    ((SELECT id FROM produtos WHERE sku = 'TIN-PRE-ES-1L'), NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 10, 'Entrada de estoque inicial', null),
    ((SELECT id FROM produtos WHERE sku = 'FITA-DF-25MM'), NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 5, 'Entrada de estoque inicial', null),
    ((SELECT id FROM produtos WHERE sku = 'ILHOS-BNR-100'), NOW() - INTERVAL '1 day', 'ENTRADA_PRODUCAO', 20, 'Entrada de estoque inicial', null),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), NOW() - INTERVAL '20 hour', 'ENTRADA_PRODUCAO', 8, 'Produzido pela Ordem LOTE-INT-RESINA-201', (SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201')),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), NOW() - INTERVAL '18 hour', 'ENTRADA_PRODUCAO', 15, 'Produzido pela Ordem REPOSICAO-DTF-305', (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305')),
    ((SELECT id FROM produtos WHERE sku = 'VERN-UV-250'), NOW() - INTERVAL '16 hour', 'ENTRADA_PRODUCAO', 12, 'Produzido pela Ordem REPOSICAO-UV-410', (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410')),
    ((SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'), NOW() - INTERVAL '14 hour', 'ENTRADA_PRODUCAO', 20, 'Produzido pela Ordem ESTOQUE-PAPEL-SEDA-112', (SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112')),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), NOW() - INTERVAL '3 hour', 'ENTRADA_PRODUCAO', 50, 'Produzido pela Ordem #3', (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Estorno Válido')),
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), NOW() - INTERVAL '2 hour', 'ENTRADA_PRODUCAO', 1000, 'Produzido pela Ordem #4', (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Venda')),
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), NOW() - INTERVAL '1 hour', 'SAIDA_VENDA', -200, 'Venda #V555', null),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), NOW() - INTERVAL '1 hour', 'ENTRADA_PRODUCAO', 20, 'Produzido pela Ordem #5', (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Geradora')),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), NOW() - INTERVAL '10 hour', 'SAIDA_VENDA', -1, 'Venda de balcão #RES-201', null),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), NOW() - INTERVAL '8 hour', 'SAIDA_VENDA', -2, 'Pedido e-commerce #DTF-887', null);

-- Histórico de Movimentações de Estoque de Lote (dependem de Lotes e Ordens de Produção)
INSERT INTO movimentacoes_estoque_lote (lote_id, ordem_producao_id, data, tipo, quantidade, motivo) VALUES
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1003'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Estorno Válido'), NOW() - INTERVAL '3 hour', 'SAIDA_PRODUCAO', -1, 'Consumo para Ordem #3'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Retalho da Ordem #3'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Estorno Válido'), NOW() - INTERVAL '3 hour', 'ENTRADA_SOBRA', 0.2, 'Retalho gerado pela Ordem #3'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1001'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Venda'), NOW() - INTERVAL '2 hour', 'SAIDA_PRODUCAO', -20, 'Consumo para Ordem #4'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1002'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Geradora'), NOW() - INTERVAL '1 hour', 'SAIDA_PRODUCAO', -25, 'Consumo para Ordem #5'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Retalho da Ordem #5'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Geradora'), NOW() - INTERVAL '1 hour', 'ENTRADA_SOBRA', 5, 'Retalho gerado pela Ordem #5'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Retalho da Ordem #5'), (SELECT id FROM ordens_de_producao WHERE motivo = 'Teste Bloqueio por Retalho - Consumidora'), NOW() - INTERVAL '30 minute', 'SAIDA_PRODUCAO', -1, 'Consumo do retalho da Ordem #5'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1009'), (SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201'), NOW() - INTERVAL '20 hour', 'SAIDA_PRODUCAO', -16, 'Consumo para Ordem LOTE-INT-RESINA-201'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1010'), (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305'), NOW() - INTERVAL '18 hour', 'SAIDA_PRODUCAO', -7500, 'Consumo para Ordem REPOSICAO-DTF-305'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1011'), (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410'), NOW() - INTERVAL '16 hour', 'SAIDA_PRODUCAO', -3000, 'Consumo para Ordem REPOSICAO-UV-410'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1012'), (SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112'), NOW() - INTERVAL '14 hour', 'SAIDA_PRODUCAO', -2000, 'Consumo para Ordem ESTOQUE-PAPEL-SEDA-112');
