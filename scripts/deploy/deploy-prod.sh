#!/usr/bin/env bash
set -euo pipefail

# scripts/deploy/deploy-prod.sh
# Sobe a stack de produção local (teste) usando caminhos reais: /etc e /opt.
# Uso: sudo ./scripts/deploy/deploy-prod.sh [caminho_para_.env.prod]

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

# Se um arquivo .env foi passado, instale-o em /etc/dcriar/.env.prod
if [ "${1-}" != "" ]; then
  echo "Instalando .env.prod em /etc/dcriar/.env.prod..."
  sudo ./scripts/deploy/install-prod-env.sh "$1"
fi

PROD_ENV_FILE="/etc/dcriar/.env.prod"
PROD_COMPOSE_FILE="/opt/dcriar/docker-compose.prod.yml"
PROJECT_NAME="dcriar-prod"
COMPOSE_RUN_CMD="./scripts/lib/compose-run.sh"

# Validações
if [ ! -f "$PROD_ENV_FILE" ]; then
  echo "Erro: $PROD_ENV_FILE não encontrado. Rode install-prod-env.sh primeiro ou passe um caminho como argumento." >&2
  exit 2
fi
if [ ! -f "$PROD_COMPOSE_FILE" ]; then
  echo "Erro: $PROD_COMPOSE_FILE não encontrado. Rode sudo ./scripts/deploy/install-prod-compose.sh ./docker-compose.prod.yml" >&2
  exit 3
fi

# Pull e Up
echo "INFO: Fazendo pull das imagens..."
sudo "$COMPOSE_RUN_CMD" \
  --project-name "$PROJECT_NAME" \
  --env-file "$PROD_ENV_FILE" \
  --compose-file "$PROD_COMPOSE_FILE" \
  pull || true

echo "INFO: Subindo stack de produção (-d)..."
sudo "$COMPOSE_RUN_CMD" \
  --project-name "$PROJECT_NAME" \
  --env-file "$PROD_ENV_FILE" \
  --compose-file "$PROD_COMPOSE_FILE" \
  up -d

echo "INFO: Deploy concluído. Status:"
sudo "$COMPOSE_RUN_CMD" \
  --project-name "$PROJECT_NAME" \
  --env-file "$PROD_ENV_FILE" \
  --compose-file "$PROD_COMPOSE_FILE" \
  ps
