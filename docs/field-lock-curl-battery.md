# Bateria de CURLs: Bloqueio de Campos Sensíveis

Este arquivo reúne uma bateria de testes manuais para validar o que foi implementado em:

- `Produto`
- `Tipo de Matéria-Prima`
- `Lote de Matéria-Prima`

Objetivos:

- confirmar que os campos bloqueados continuam vindo na API
- confirmar que campos sensíveis dão `409 Conflict`
- confirmar que campos não sensíveis continuam editáveis
- validar erros intencionais (`400`, `404`, `409`)
- exercitar alguns casos de borda

## Pré-requisitos

- backend rodando em `http://localhost:8080`
- base seeded carregada
- IDs conhecidos da base seeded:
  - `Produto` usado: `1`
  - `Tipo de Matéria-Prima` usado: `1`
  - `Lote` usado: `1`

## Variáveis úteis

```bash
BASE_URL="http://localhost:8080"
PRODUCT_ID=1
MATERIAL_TYPE_ID=1
BATCH_ID=1
```

## 1. Leitura e descoberta

### 1.1 Produto em uso deve expor campos bloqueados

Esperado:
- `200 OK`
- `camposBloqueados` preenchido
- `motivosBloqueio` preenchido

```bash
curl -i "$BASE_URL/api/v1/produtos/$PRODUCT_ID"
```

### 1.2 Tipo de matéria-prima em uso deve expor campos bloqueados

Esperado:
- `200 OK`
- `camposBloqueados` com `unidadeDeConsumo`

```bash
curl -i "$BASE_URL/api/v1/tipos-materia-prima/$MATERIAL_TYPE_ID"
```

### 1.3 Lote em uso deve expor campos bloqueados

Esperado:
- `200 OK`
- `camposBloqueados` com campos estruturais

```bash
curl -i "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID"
```

### 1.4 Listagens devem continuar funcionando

Esperado:
- `200 OK`
- cada item pode ou não ter `camposBloqueados`

```bash
curl -i "$BASE_URL/api/v1/produtos?page=0&size=10&sort=nome,asc"
curl -i "$BASE_URL/api/v1/tipos-materia-prima?page=0&size=10&sort=nome,asc"
curl -i "$BASE_URL/api/v1/lotes-materia-prima?page=0&size=10&sort=tipoMateriaPrima.nome,asc"
```

## 2. Produto: sucesso e conflito

### 2.1 PATCH em campo não sensível deve funcionar

Observação:
- este teste altera a descrição e depois restaura

Esperado:
- `200 OK`

```bash
curl -i -X PATCH "$BASE_URL/api/v1/produtos/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Descricao temporaria para teste de bloqueio estrutural."}'
```

### 2.2 Restaurar descrição do produto

```bash
curl -i -X PATCH "$BASE_URL/api/v1/produtos/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{"descricao":"Cartão de visita em papel couchê 300g, laminação fosca."}'
```

### 2.3 PATCH em `unidadesPorProduto` deve falhar com `409`

Esperado:
- `409 Conflict`
- `details.camposBloqueados` com `unidadesPorProduto`

```bash
curl -i -X PATCH "$BASE_URL/api/v1/produtos/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{"unidadesPorProduto":200}'
```

### 2.4 PATCH em `tipoMateriaPrimaId` deve falhar com `409`

```bash
curl -i -X PATCH "$BASE_URL/api/v1/produtos/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{"tipoMateriaPrimaId":2}'
```

### 2.5 PATCH em `dimensoes` deve falhar com `409`

```bash
curl -i -X PATCH "$BASE_URL/api/v1/produtos/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{"dimensoes":{"larguraCm":10,"comprimentoCm":6}}'
```

### 2.6 PATCH vazio deve ser no-op

Esperado:
- `200 OK`
- sem erro

```bash
curl -i -X PATCH "$BASE_URL/api/v1/produtos/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{}'
```

## 3. Tipo de Matéria-Prima: sucesso e conflito

### 3.1 PATCH em nome deve funcionar

Observação:
- altera e depois restaura

```bash
curl -i -X PATCH "$BASE_URL/api/v1/tipos-materia-prima/$MATERIAL_TYPE_ID" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Papel Couchê 300g TESTE"}'
```

### 3.2 Restaurar nome do tipo

```bash
curl -i -X PATCH "$BASE_URL/api/v1/tipos-materia-prima/$MATERIAL_TYPE_ID" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Papel Couchê 300g"}'
```

### 3.3 PATCH em `unidadeDeConsumo` deve falhar com `409`

```bash
curl -i -X PATCH "$BASE_URL/api/v1/tipos-materia-prima/$MATERIAL_TYPE_ID" \
  -H "Content-Type: application/json" \
  -d '{"unidadeDeConsumo":"LITRO"}'
```

## 4. Lote: sucesso e conflito

### 4.1 PUT alterando só `motivo` deve funcionar

Observação:
- altera e depois restaura

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":150,
    "motivo":"Motivo temporario para teste de bloqueio estrutural.",
    "atributos":{"larguraMm":320,"comprimentoMm":450}
  }'
```

### 4.2 Restaurar motivo do lote

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":150,
    "motivo":"Compra NF-1001",
    "atributos":{"larguraMm":320,"comprimentoMm":450}
  }'
```

### 4.3 PUT alterando `custoTotalLote` deve falhar com `409`

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":151,
    "motivo":"Compra NF-1001",
    "atributos":{"larguraMm":320,"comprimentoMm":450}
  }'
```

### 4.4 PUT alterando `atributos.larguraMm` deve falhar com `409`

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":150,
    "motivo":"Compra NF-1001",
    "atributos":{"larguraMm":321,"comprimentoMm":450}
  }'
```

### 4.5 PUT alterando só atributo descritivo deve funcionar

Observação:
- este teste valida que atributos gerais continuam livres

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":150,
    "motivo":"Compra NF-1001",
    "atributos":{"larguraMm":320,"comprimentoMm":450,"fornecedor":"Fornecedor Teste"}
  }'
```

### 4.6 Restaurar atributo descritivo

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":150,
    "motivo":"Compra NF-1001",
    "atributos":{"larguraMm":320,"comprimentoMm":450}
  }'
```

## 5. Erros intencionais de validação

### 5.1 Criar tipo de matéria-prima inválido

Esperado:
- `400 Bad Request`

```bash
curl -i -X POST "$BASE_URL/api/v1/tipos-materia-prima" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 5.2 Criar produto inválido

Esperado:
- `400 Bad Request`

```bash
curl -i -X POST "$BASE_URL/api/v1/produtos" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### 5.3 Criar lote inválido

Esperado:
- `400 Bad Request`

```bash
curl -i -X POST "$BASE_URL/api/v1/lotes-materia-prima" \
  -H "Content-Type: application/json" \
  -d '{}'
```

## 6. Erros intencionais de não encontrado

### 6.1 Produto inexistente

```bash
curl -i "$BASE_URL/api/v1/produtos/999999"
```

### 6.2 Tipo de matéria-prima inexistente

```bash
curl -i "$BASE_URL/api/v1/tipos-materia-prima/999999"
```

### 6.3 Lote inexistente

```bash
curl -i "$BASE_URL/api/v1/lotes-materia-prima/999999"
```

## 7. Casos de borda úteis

### 7.1 PATCH de produto com campo sensível e não sensível no mesmo request

Esperado:
- `409 Conflict`
- nada deve ser atualizado

```bash
curl -i -X PATCH "$BASE_URL/api/v1/produtos/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "descricao":"Descricao que nao deveria persistir.",
    "unidadesPorProduto":300
  }'
```

### 7.2 PUT de lote com campos estruturais inalterados e motivo alterado

Este é o caso importante para garantir que a proteção não bloqueia `PUT` só porque os campos sensíveis estão presentes no payload.

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":150,
    "motivo":"Teste de PUT com campos estruturais repetidos sem alteracao.",
    "atributos":{"larguraMm":320,"comprimentoMm":450}
  }'
```

### 7.3 Restaurar motivo após o teste acima

```bash
curl -i -X PUT "$BASE_URL/api/v1/lotes-materia-prima/$BATCH_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "tipoMateriaPrimaId":1,
    "unidadeDeEstoque":"METRO_QUADRADO",
    "unidadeCadastroEstoque":"METRO_QUADRADO",
    "quantidadeInicial":480,
    "custoTotalLote":150,
    "motivo":"Compra NF-1001",
    "atributos":{"larguraMm":320,"comprimentoMm":450}
  }'
```

## 8. Histórico e regressão operacional

Estes testes não são só de bloqueio, mas ajudam a ver se algo colateral quebrou:

### 8.1 Histórico consolidado continua respondendo

```bash
curl -i "$BASE_URL/api/v1/estoques/historico?page=0&size=5&sort=data,desc&periodo=all"
```

### 8.2 Busca sem acento no histórico continua funcionando

```bash
curl -i "$BASE_URL/api/v1/estoques/historico?page=0&size=5&sort=produto.nome,asc&periodo=all&nomeProduto=cartao"
```

### 8.3 Filtro por movimentação continua funcionando

```bash
curl -i "$BASE_URL/api/v1/estoques/historico?page=0&size=5&periodo=all&tipoMovimentacao=AJUSTE_MANUAL"
```

## 9. Checklist rápido

Se essa bateria passar, o mínimo esperado é:

- leitura dos bloqueios funcionando
- conflitos `409` corretos
- campos livres ainda editáveis
- `PUT` de lote não bloqueando falso positivo
- validações `400` continuam ativas
- `404` continuam corretos
- histórico de estoque não regrediu
