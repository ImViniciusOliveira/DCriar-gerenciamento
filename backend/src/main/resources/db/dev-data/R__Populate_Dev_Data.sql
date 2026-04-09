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
    RESTART IDENTITY CASCADE;

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
INSERT INTO lotes_materia_prima (tipo_materia_prima_id, unidade_de_estoque, unidade_cadastro_estoque, custo_total_lote, motivo, atributos, data_criacao, data_atualizacao) VALUES
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Couchê 300g'), 'METRO_QUADRADO', 'METRO_QUADRADO', 150.00, 'Compra NF-1001', '{ "larguraMm": 320, "comprimentoMm": 450 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Lona Fosca 440g'), 'METRO_QUADRADO', 'METRO_QUADRADO', 250.00, 'Compra NF-1002', '{ "larguraMm": 1600 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo Vinil Branco'), 'METRO_QUADRADO', 'METRO_QUADRADO', 120.00, 'Compra NF-1003', '{ "larguraMm": 1200 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo BOPP Transparente'), 'METRO_QUADRADO', 'METRO_QUADRADO', 80.00, 'Compra NF-1004', '{ "larguraMm": 297, "comprimentoMm": 420 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Kraft 180g'), 'METRO_QUADRADO', 'METRO_QUADRADO', 50.00, 'Compra NF-1005', '{ "larguraMm": 210, "comprimentoMm": 297 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Tinta Eco-Solvente Preta'), 'LITRO', 'LITRO', 300.00, 'Compra NF-1006', '{ "fornecedor": "InkMaster" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Fita Dupla Face 25mm'), 'METRO_LINEAR', 'METRO_LINEAR', 75.00, 'Compra NF-1007', '{ "larguraMm": 25, "metragem_m": 50 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Ilhós de Latão #0'), 'UNIDADE', 'UNIDADE', 200.00, 'Compra NF-1008', '{ "quantidade_caixa": 1000 }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Resina Epóxi Transparente'), 'QUILOGRAMA', 'QUILOGRAMA', 1800.00, 'Compra NF-1009', '{ "fornecedor": "Quimicolor", "lote_fabricante": "EPX-2309" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Pó Adesivo Termocolante'), 'GRAMA', 'GRAMA', 950.00, 'Compra NF-1010', '{ "fornecedor": "PrintBond", "malha": "fina" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Verniz UV Brilhante'), 'MILILITRO', 'MILILITRO', 700.00, 'Compra NF-1011', '{ "fornecedor": "UV Coatings", "acabamento": "brilho" }', NOW(), NOW()),
    ((SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Seda Branco A4'), 'FOLHA', 'FOLHA', 250.00, 'Compra NF-1012', '{ "gramatura_gm2": 18, "cor": "branco" }', NOW(), NOW());

-- Inserção de Produtos Acabados (dependem de Tipos de Matérias-Primas)
INSERT INTO produtos (tipo_produto, nome, sku, descricao, cor, unidades_por_produto, ativo, foto_principal_url, tipo_materia_prima_id, largura_cm, comprimento_cm, data_criacao, data_atualizacao, codigo_fabricante, unidade_cadastro_consumo, especificacoes) VALUES
    ('CORTE', 'Cartão de Visita Premium', 'CV-PREM-9X5', 'Cartão de visita em papel couchê 300g, laminação fosca.', 'Branco', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Couchê 300g'), 9.0, 5.0, NOW(), NOW(), null, null, null),
    ('CORTE', 'Banner Comercial 1,20x0,80m', 'BNR-COM-120X80', 'Banner em lona fosca 440g com bastão e corda.', 'Personalizada', 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Lona Fosca 440g'), 80.0, 120.0, NOW(), NOW(), null, null, null),
    ('CORTE', 'Adesivo Redondo 5cm', 'ADSV-RD-5', 'Adesivo em vinil branco para uso geral, corte redondo.', 'Branco', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo Vinil Branco'), 5.0, 5.0, NOW(), NOW(), null, null, null),
    ('CORTE', 'Folder A4 Dobrado', 'FLD-A4-DOB', 'Folder promocional em papel couchê 300g com dobra central.', 'Colorido', 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Couchê 300g'), 21.0, 29.7, NOW(), NOW(), null, null, null),
    ('CORTE', 'Rótulo para Cerveja Long Neck', 'ROT-CERV-LN', 'Rótulo para garrafas, resistente à umidade, em BOPP transparente.', 'Transparente', 50, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo BOPP Transparente'), 8.0, 7.0, NOW(), NOW(), null, null, null),
    ('CORTE', 'Adesivo Holográfico 10x10cm', 'ADSV-HOLO-10', 'Adesivo com acabamento holográfico para brindes e embalagens.', 'Holográfico', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Adesivo Vinil Branco'), 10.0, 10.0, NOW(), NOW(), null, null, null),
    ('CORTE', 'Tag Kraft para Roupas 4x9cm', 'TAG-KFT-4X9', 'Tag em papel kraft 180g com furo para aplicação em peças de vestuário.', 'Pardo', 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Kraft 180g'), 4.0, 9.0, NOW(), NOW(), null, null, null),
    ('CONSUMO', 'Tinta Eco-Solvente Preta', 'TIN-PRE-ES-1L', 'Tinta preta para impressoras eco-solvente, frasco com 1 litro.', null, 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Tinta Eco-Solvente Preta'), null, null, NOW(), NOW(), 'INK-BLK-ES-1L', 'LITRO', '{"tipo_tinta": "Eco-Solvente", "cor_pantone": "Black C", "volume_ml": 1000}'),
    ('CORTE', 'Fita Dupla Face 25mm x 50m', 'FITA-DF-25MM', 'Rolo de fita dupla face de alta aderência com 25 mm de largura e 50 metros de comprimento.', 'Transparente', 1, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Fita Dupla Face 25mm'), 2.5, 5000.0, NOW(), NOW(), null, null, null),
    ('CONSUMO', 'Pacote de Ilhós Nº 0', 'ILHOS-BNR-100', 'Pacote com 100 unidades de ilhós de latão número 0 para acabamento de banners.', null, 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Ilhós de Latão #0'), null, null, NOW(), NOW(), 'ILHOS-LT-0', 'UNIDADE', '{"diametro_mm": 10, "material": "Latão"}'),
    ('CONSUMO', 'Kit de Resina Epóxi', 'RES-EPX-2KG', 'Kit de resina epóxi transparente para artesanato e encapsulamento, com 2 kg.', null, 2, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Resina Epóxi Transparente'), null, null, NOW(), NOW(), 'EPX-KIT-2KG', 'QUILOGRAMA', '{"acabamento": "transparente", "uso": "artesanato"}'),
    ('CONSUMO', 'Refil de Pó Adesivo', 'PO-ADT-500G', 'Refil de pó adesivo termocolante para DTF, embalagem com 500 gramas.', null, 500, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Pó Adesivo Termocolante'), null, null, NOW(), NOW(), 'DTF-PO-500', 'GRAMA', '{"malha": "fina", "uso": "transfer"}'),
    ('CONSUMO', 'Frasco de Verniz UV', 'VERN-UV-250', 'Frasco de verniz UV brilhante para acabamento gráfico, com 250 ml.', null, 250, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Verniz UV Brilhante'), null, null, NOW(), NOW(), 'UV-BRILHO-250', 'MILILITRO', '{"acabamento": "brilhante", "cura": "UV"}'),
    ('CONSUMO', 'Pacote de Papel Seda A4', 'PAP-SEDA-A4-100', 'Pacote de papel seda branco A4 com 100 folhas para proteção e acabamento.', null, 100, true, '', (SELECT id FROM tipos_materia_prima WHERE nome = 'Papel Seda Branco A4'), null, null, NOW(), NOW(), 'SEDA-A4-100', 'FOLHA', '{"cor": "branco", "gramatura_gm2": 18}');

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
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 3800),
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 1000),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), (SELECT id FROM canais_venda WHERE nome = 'Equipe de Vendas'), 10),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), (SELECT id FROM canais_venda WHERE nome = 'Shopee'), 500),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), (SELECT id FROM canais_venda WHERE nome = 'Mercado Livre'), 500),
    ((SELECT id FROM produtos WHERE sku = 'ROT-CERV-LN'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 250),
    ((SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 500),
    ((SELECT id FROM produtos WHERE sku = 'TIN-PRE-ES-1L'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 10),
    ((SELECT id FROM produtos WHERE sku = 'FITA-DF-25MM'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 5),
    ((SELECT id FROM produtos WHERE sku = 'ILHOS-BNR-100'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 20),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 7),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 13),
    ((SELECT id FROM produtos WHERE sku = 'VERN-UV-250'), (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 12),
    ((SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'), (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 20);

-- Inserção de Preços (dependem de Produtos)
INSERT INTO precos (produto_id, valor, data_criacao, data_atualizacao) VALUES
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), 120.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), 85.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), 45.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'FLD-A4-DOB'), 15.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ROT-CERV-LN'), 60.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ADSV-HOLO-10'), 55.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'), 30.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'TIN-PRE-ES-1L'), 350.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'FITA-DF-25MM'), 75.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'ILHOS-BNR-100'), 50.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), 189.90, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), 79.90, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'VERN-UV-250'), 45.00, NOW(), NOW()),
    ((SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'), 35.00, NOW(), NOW());

-- ETAPA 5: INSERÇÃO DE DADOS DE PRODUÇÃO E VENDAS (Nível 3 - dependem de tudo acima)
-- Estas são as tabelas transacionais que representam as operações do dia a dia.

-- Inserção de Ordens de Produção (dependem de Produtos e Canais de Venda)
INSERT INTO ordens_de_producao (produto_id, quantidade_produzida, modo_calculo, largura_final_cm, comprimento_final_cm, data_criacao, data_atualizacao, motivo, canal_venda_destino_id, margem_superior_cm, margem_inferior_cm, margem_esquerda_cm, margem_direita_cm) VALUES
    ((SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), 100, 'AUTOMATICO', 5.2, 500.4, NOW() - INTERVAL '2 day', NOW() - INTERVAL '2 day', 'PEDIDO-SHP-101', (SELECT id FROM canais_venda WHERE nome = 'Shopee'), 0.2, 0.2, 0.1, 0.1),
    ((SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), 50, 'MANUAL', 10.0, 260.0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 'PEDIDO-LJA-205', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 0.0, 0.0, 0.0, 0.0),
    ((SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), 8, null, null, null, NOW() - INTERVAL '20 hour', NOW() - INTERVAL '20 hour', 'LOTE-INT-RESINA-201', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), null, null, null, null),
    ((SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), 15, null, null, null, NOW() - INTERVAL '18 hour', NOW() - INTERVAL '18 hour', 'REPOSICAO-DTF-305', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), null, null, null, null),
    ((SELECT id FROM produtos WHERE sku = 'VERN-UV-250'), 12, null, null, null, NOW() - INTERVAL '16 hour', NOW() - INTERVAL '16 hour', 'REPOSICAO-UV-410', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), null, null, null, null),
    ((SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'), 20, null, null, null, NOW() - INTERVAL '14 hour', NOW() - INTERVAL '14 hour', 'ESTOQUE-PAPEL-SEDA-112', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), null, null, null, null);

-- Inserção de Lotes de Retalho (dependem de Lotes de Compra e Ordens de Produção)
-- É crucial que esta etapa venha DEPOIS da criação das Ordens de Produção.
INSERT INTO lotes_materia_prima (tipo_materia_prima_id, unidade_de_estoque, unidade_cadastro_estoque, custo_total_lote, motivo, atributos, lote_de_origem_id, ordem_producao_origem_id, data_criacao, data_atualizacao)
SELECT NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE FALSE;

-- Inserção de Vendas (dependem de Canais de Venda)
-- Observação:
-- - esta carga SQL grava direto no banco e não passa pelo conversor JPA de criptografia
-- - por isso, aqui populamos apenas os campos de cliente que permanecem em claro
-- - campos criptografados em repouso (nome_completo, endereco, numero, bairro, cep, cpf, observacao)
--   devem continuar nulos nesta carga repetível
INSERT INTO vendas (data_criacao, data_atualizacao, canal_venda_id, valor_total, apelido, cidade, estado) VALUES
    (NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 99.90, 'Cliente Premium', 'Sao Paulo', 'Sao Paulo'),
    (NOW() - INTERVAL '12 hour', NOW() - INTERVAL '12 hour', (SELECT id FROM canais_venda WHERE nome = 'Equipe de Vendas'), 170.00, 'Equipe Alpha', 'Campinas', 'Sao Paulo'),
    (NOW(), NOW(), (SELECT id FROM canais_venda WHERE nome = 'Shopee'), 75.00, 'Shopee Julia', 'Curitiba', 'Parana'),
    (NOW() - INTERVAL '10 hour', NOW() - INTERVAL '10 hour', (SELECT id FROM canais_venda WHERE nome = 'Loja Física'), 189.90, 'Cliente Balcao', 'Belo Horizonte', 'Minas Gerais'),
    (NOW() - INTERVAL '8 hour', NOW() - INTERVAL '8 hour', (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'), 159.80, 'Studio DTF', 'Rio de Janeiro', 'Rio de Janeiro');

-- ETAPA 6: INSERÇÃO DE DADOS DE RELACIONAMENTO E HISTÓRICO (Nível Final)
-- Estas são as tabelas de junção e logs que dependem de todas as outras entidades.

-- Vínculos de Consumo (Ordem -> Lote)
INSERT INTO ordem_producao_lotes_consumidos (ordem_producao_id, lote_materia_prima_id) VALUES
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1001')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1002')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1009')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1010')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1011')),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112'), (SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1012'));

-- Cortes Realizados (dependem de Ordens de Produção)
INSERT INTO cortes_realizados (ordem_de_producao_id, largura_cm, comprimento_cm, quantidade, tipo, retalho_categoria) VALUES
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101'), 5.0, 5.0, 100, 'PRODUTO', NULL),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205'), 9.0, 5.0, 50, 'PRODUTO', NULL),
    ((SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112'), 21.0, 29.7, 20, 'PRODUTO', NULL);

-- Itens de Venda (dependem de Vendas e Produtos)
INSERT INTO itens_venda (venda_id, produto_id, quantidade, preco_comercial_original, preco_unitario, preco_total, tipo_preco_aplicado, motivo_alteracao_preco) VALUES
    ((SELECT id FROM vendas WHERE valor_total = 99.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio')), (SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'), 1, 120.00, 99.90, 99.90, 'PRECO_ALTERADO', 'Cliente recorrente'),
    ((SELECT id FROM vendas WHERE valor_total = 170.00 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Equipe de Vendas')), (SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'), 2, 85.00, 85.00, 170.00, 'PRECO_PADRAO', NULL),
    ((SELECT id FROM vendas WHERE valor_total = 75.00 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Shopee')), (SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'), 1, 45.00, 45.00, 45.00, 'PRECO_PADRAO', NULL),
    ((SELECT id FROM vendas WHERE valor_total = 75.00 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Shopee')), (SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'), 1, 30.00, 30.00, 30.00, 'PRECO_PADRAO', NULL),
    ((SELECT id FROM vendas WHERE valor_total = 189.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Loja Física')), (SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'), 1, 189.90, 189.90, 189.90, 'PRECO_PADRAO', NULL),
    ((SELECT id FROM vendas WHERE valor_total = 159.80 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio')), (SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'), 2, 79.90, 79.90, 159.80, 'PRECO_PADRAO', NULL);

-- Histórico de Movimentações de Estoque de Produto (dependem de Produtos, Ordens de Produção e Vendas)
-- Observação:
-- - ordens de produção reais preenchem ordem_producao_id e ordem_producao_origem_id
-- - vendas preenchem venda_origem_id para alimentar a auditoria do histórico consolidado
-- - cargas iniciais de produtos sem OP foram convertidas para AJUSTE_MANUAL, que descreve melhor a origem do saldo
INSERT INTO movimentacoes_estoque_produto (
    produto_id, produto_nome_snapshot, produto_sku_snapshot, data, tipo, quantidade, motivo, ordem_producao_id, ordem_producao_origem_id, venda_origem_id
) VALUES
    (
        (SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'),
        (SELECT nome FROM produtos WHERE sku = 'CV-PREM-9X5'),
        (SELECT sku FROM produtos WHERE sku = 'CV-PREM-9X5'),
        NOW() - INTERVAL '5 day',
        'ENTRADA_PRODUCAO',
        5000,
        CONCAT('Lançamento da OP #', (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101')),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101'),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-SHP-101'),
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'BNR-COM-120X80'),
        (SELECT nome FROM produtos WHERE sku = 'BNR-COM-120X80'),
        (SELECT sku FROM produtos WHERE sku = 'BNR-COM-120X80'),
        NOW() - INTERVAL '4 day',
        'ENTRADA_PRODUCAO',
        50,
        CONCAT('Lançamento da OP #', (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205')),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205'),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'PEDIDO-LJA-205'),
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'ADSV-RD-5'),
        (SELECT nome FROM produtos WHERE sku = 'ADSV-RD-5'),
        (SELECT sku FROM produtos WHERE sku = 'ADSV-RD-5'),
        NOW() - INTERVAL '3 day',
        'AJUSTE_MANUAL',
        1000,
        'Ajuste inicial do estoque para carga de dados',
        NULL,
        NULL,
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'ROT-CERV-LN'),
        (SELECT nome FROM produtos WHERE sku = 'ROT-CERV-LN'),
        (SELECT sku FROM produtos WHERE sku = 'ROT-CERV-LN'),
        NOW() - INTERVAL '2 day',
        'AJUSTE_MANUAL',
        250,
        'Ajuste inicial do estoque para carga de dados',
        NULL,
        NULL,
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'TAG-KFT-4X9'),
        (SELECT nome FROM produtos WHERE sku = 'TAG-KFT-4X9'),
        (SELECT sku FROM produtos WHERE sku = 'TAG-KFT-4X9'),
        NOW() - INTERVAL '1 day',
        'AJUSTE_MANUAL',
        500,
        'Ajuste inicial do estoque para carga de dados',
        NULL,
        NULL,
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'TIN-PRE-ES-1L'),
        (SELECT nome FROM produtos WHERE sku = 'TIN-PRE-ES-1L'),
        (SELECT sku FROM produtos WHERE sku = 'TIN-PRE-ES-1L'),
        NOW() - INTERVAL '1 day',
        'AJUSTE_MANUAL',
        10,
        'Ajuste inicial do estoque para carga de dados',
        NULL,
        NULL,
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'FITA-DF-25MM'),
        (SELECT nome FROM produtos WHERE sku = 'FITA-DF-25MM'),
        (SELECT sku FROM produtos WHERE sku = 'FITA-DF-25MM'),
        NOW() - INTERVAL '1 day',
        'AJUSTE_MANUAL',
        5,
        'Ajuste inicial do estoque para carga de dados',
        NULL,
        NULL,
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'ILHOS-BNR-100'),
        (SELECT nome FROM produtos WHERE sku = 'ILHOS-BNR-100'),
        (SELECT sku FROM produtos WHERE sku = 'ILHOS-BNR-100'),
        NOW() - INTERVAL '1 day',
        'AJUSTE_MANUAL',
        20,
        'Ajuste inicial do estoque para carga de dados',
        NULL,
        NULL,
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'),
        (SELECT nome FROM produtos WHERE sku = 'RES-EPX-2KG'),
        (SELECT sku FROM produtos WHERE sku = 'RES-EPX-2KG'),
        NOW() - INTERVAL '20 hour',
        'ENTRADA_PRODUCAO',
        8,
        CONCAT('Lançamento da OP #', (SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201')),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201'),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201'),
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'),
        (SELECT nome FROM produtos WHERE sku = 'PO-ADT-500G'),
        (SELECT sku FROM produtos WHERE sku = 'PO-ADT-500G'),
        NOW() - INTERVAL '18 hour',
        'ENTRADA_PRODUCAO',
        15,
        CONCAT('Lançamento da OP #', (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305')),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305'),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305'),
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'VERN-UV-250'),
        (SELECT nome FROM produtos WHERE sku = 'VERN-UV-250'),
        (SELECT sku FROM produtos WHERE sku = 'VERN-UV-250'),
        NOW() - INTERVAL '16 hour',
        'ENTRADA_PRODUCAO',
        12,
        CONCAT('Lançamento da OP #', (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410')),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410'),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410'),
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'PAP-SEDA-A4-100'),
        (SELECT nome FROM produtos WHERE sku = 'PAP-SEDA-A4-100'),
        (SELECT sku FROM produtos WHERE sku = 'PAP-SEDA-A4-100'),
        NOW() - INTERVAL '14 hour',
        'ENTRADA_PRODUCAO',
        20,
        CONCAT('Lançamento da OP #', (SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112')),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112'),
        (SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112'),
        NULL
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'CV-PREM-9X5'),
        (SELECT nome FROM produtos WHERE sku = 'CV-PREM-9X5'),
        (SELECT sku FROM produtos WHERE sku = 'CV-PREM-9X5'),
        NOW() - INTERVAL '1 hour',
        'SAIDA_VENDA',
        -200,
        CONCAT('Venda #', (SELECT id FROM vendas WHERE valor_total = 99.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'))),
        NULL,
        NULL,
        (SELECT id FROM vendas WHERE valor_total = 99.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'))
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'RES-EPX-2KG'),
        (SELECT nome FROM produtos WHERE sku = 'RES-EPX-2KG'),
        (SELECT sku FROM produtos WHERE sku = 'RES-EPX-2KG'),
        NOW() - INTERVAL '10 hour',
        'SAIDA_VENDA',
        -1,
        CONCAT('Venda #', (SELECT id FROM vendas WHERE valor_total = 189.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Loja Física'))),
        NULL,
        NULL,
        (SELECT id FROM vendas WHERE valor_total = 189.90 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Loja Física'))
    ),
    (
        (SELECT id FROM produtos WHERE sku = 'PO-ADT-500G'),
        (SELECT nome FROM produtos WHERE sku = 'PO-ADT-500G'),
        (SELECT sku FROM produtos WHERE sku = 'PO-ADT-500G'),
        NOW() - INTERVAL '8 hour',
        'SAIDA_VENDA',
        -2,
        CONCAT('Venda #', (SELECT id FROM vendas WHERE valor_total = 159.80 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'))),
        NULL,
        NULL,
        (SELECT id FROM vendas WHERE valor_total = 159.80 AND canal_venda_id = (SELECT id FROM canais_venda WHERE nome = 'Site Próprio'))
    );

-- Histórico de Movimentações de Estoque de Lote (dependem de Lotes e Ordens de Produção)
INSERT INTO movimentacoes_estoque_lote (lote_id, ordem_producao_id, data, tipo, quantidade, motivo) VALUES
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1009'), (SELECT id FROM ordens_de_producao WHERE motivo = 'LOTE-INT-RESINA-201'), NOW() - INTERVAL '20 hour', 'SAIDA_PRODUCAO', -16, 'Consumo para Ordem LOTE-INT-RESINA-201'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1010'), (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-DTF-305'), NOW() - INTERVAL '18 hour', 'SAIDA_PRODUCAO', -7500, 'Consumo para Ordem REPOSICAO-DTF-305'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1011'), (SELECT id FROM ordens_de_producao WHERE motivo = 'REPOSICAO-UV-410'), NOW() - INTERVAL '16 hour', 'SAIDA_PRODUCAO', -3000, 'Consumo para Ordem REPOSICAO-UV-410'),
    ((SELECT id FROM lotes_materia_prima WHERE motivo = 'Compra NF-1012'), (SELECT id FROM ordens_de_producao WHERE motivo = 'ESTOQUE-PAPEL-SEDA-112'), NOW() - INTERVAL '14 hour', 'SAIDA_PRODUCAO', -2000, 'Consumo para Ordem ESTOQUE-PAPEL-SEDA-112');
