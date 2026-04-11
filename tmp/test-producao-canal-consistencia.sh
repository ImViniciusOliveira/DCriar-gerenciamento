#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
QUANTIDADE_PRODUZIDA="${QUANTIDADE_PRODUZIDA:-1}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Comando obrigatório não encontrado: $1" >&2
    exit 1
  fi
}

require_command curl
require_command jq

echo "Base URL: $BASE_URL"

fetch_json() {
  local url="$1"
  curl -fsS "$url"
}

first_channel() {
  fetch_json "$BASE_URL/canais-venda" \
    | jq -c 'first([.. | objects | select(.id? != null and .nome? != null) | { id, nome }] | .[]) // empty'
}

find_suitable_product_and_lot() {
  local products_json
  products_json="$(fetch_json "$BASE_URL/produtos/by-tipo?tipoProduto=CONSUMO&page=0&size=100&sort=nome,asc")"

  while IFS= read -r product; do
    local product_id material_id unidades lotes_json lot
    product_id="$(jq -r '.id' <<<"$product")"
    material_id="$(jq -r '.materiaPrimaId' <<<"$product")"
    unidades="$(jq -r '.unidadesPorProduto' <<<"$product")"

    lotes_json="$(fetch_json "$BASE_URL/lotes-materia-prima?tipoMateriaPrimaId=$material_id&apenasLotesPrincipais=true&page=0&size=100&sort=id,asc")"
    lot="$(jq -c --argjson tipoMateriaPrimaId "$material_id" --argjson minimo "$unidades" '
      first(
        [
          .. | objects
          | select(.tipoMateriaPrimaId? == $tipoMateriaPrimaId)
          | select(.tipoEstrutural? == "LOTE_PRINCIPAL")
          | select((.saldoEstoque | tonumber?) >= $minimo)
          | { id, identificadorPublico, saldoEstoque }
        ] | .[]
      ) // empty
    ' <<<"$lotes_json")"

    if [[ -n "$lot" ]]; then
      jq -nc --argjson product "$product" --argjson lot "$lot" '{ product: $product, lot: $lot }'
      return 0
    fi
  done < <(
    jq -c '
      [
        .. | objects
        | select(.tipoProduto? == "CONSUMO")
        | select(.id? != null and .nome? != null)
        | select(.materiaPrima?.id? != null)
        | {
            id,
            nome,
            materiaPrimaId: .materiaPrima.id,
            unidadesPorProduto: ((.unidadesPorProduto | tonumber?) // 1)
          }
      ] | .[]
    ' <<<"$products_json"
  )

  return 1
}

read_channel_quantity() {
  local product_id="$1"
  local channel_id="$2"
  fetch_json "$BASE_URL/estoques/consultas?produtoId=$product_id&canalVendaId=$channel_id&page=0&size=20&sort=produto.nome,asc" \
    | jq -r --argjson productId "$product_id" --argjson channelId "$channel_id" '
      first(
        [
          .. | objects
          | select(.produtoId? == $productId and .canalVendaId? == $channelId)
          | .quantidadeNoCanal
        ] | .[]
      ) // 0
    '
}

channel="$(first_channel)"
if [[ -z "$channel" ]]; then
  echo "Nenhum canal de venda encontrado para o teste." >&2
  exit 1
fi

selection="$(find_suitable_product_and_lot || true)"
if [[ -z "$selection" ]]; then
  echo "Nenhum produto de consumo com lote principal compatível foi encontrado." >&2
  exit 1
fi

product_id="$(jq -r '.product.id' <<<"$selection")"
product_name="$(jq -r '.product.nome' <<<"$selection")"
units_per_product="$(jq -r '.product.unidadesPorProduto' <<<"$selection")"
lot_id="$(jq -r '.lot.id' <<<"$selection")"
lot_public_id="$(jq -r '.lot.identificadorPublico' <<<"$selection")"
lot_balance="$(jq -r '.lot.saldoEstoque' <<<"$selection")"
channel_id="$(jq -r '.id' <<<"$channel")"
channel_name="$(jq -r '.nome' <<<"$channel")"

echo "Produto selecionado: $product_name (#$product_id)"
echo "Canal selecionado: $channel_name (#$channel_id)"
echo "Lote selecionado: $lot_public_id (#$lot_id) com saldo $lot_balance"
echo "Unidades por produto: $units_per_product"

before_quantity="$(read_channel_quantity "$product_id" "$channel_id")"
echo "Quantidade no canal antes: $before_quantity"

payload="$(jq -nc \
  --argjson produtoId "$product_id" \
  --argjson loteId "$lot_id" \
  --argjson canalVendaDestinoId "$channel_id" \
  --argjson quantidadeProduzida "$QUANTIDADE_PRODUZIDA" \
  '{
    produtoId: $produtoId,
    loteId: $loteId,
    canalVendaDestinoId: $canalVendaDestinoId,
    quantidadeProduzida: $quantidadeProduzida,
    motivo: "Teste automatizado de consistencia entre producao e canal"
  }'
)"

response_file="$(mktemp)"
http_code="$(
  curl -sS -o "$response_file" -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    -d "$payload" \
    "$BASE_URL/ordens-de-producao/consumo"
)"

if [[ "$http_code" != "201" ]]; then
  echo "Falha ao criar ordem de consumo com canal. HTTP $http_code" >&2
  cat "$response_file" >&2
  rm -f "$response_file"
  exit 1
fi

after_quantity="$(read_channel_quantity "$product_id" "$channel_id")"
echo "Quantidade no canal depois: $after_quantity"

expected_quantity=$((before_quantity + QUANTIDADE_PRODUZIDA))
if [[ "$after_quantity" -ne "$expected_quantity" ]]; then
  echo "Inconsistência detectada: esperado $expected_quantity no canal, mas veio $after_quantity." >&2
  cat "$response_file" >&2
  rm -f "$response_file"
  exit 1
fi

echo "Teste OK: a ordem foi criada com canal e o estoque distribuído foi atualizado corretamente."
echo "Resposta da API:"
jq '{ id, tipoProduto, quantidadeProduzida, canalVendaDestinoId, motivo }' "$response_file"

rm -f "$response_file"
