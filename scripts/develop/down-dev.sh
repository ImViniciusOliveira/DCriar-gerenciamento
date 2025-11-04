#!/usr/bin/env bash
# scripts/develop/down-dev.sh
# Destroi o ambiente de desenvolvimento Docker Compose.

set -euo pipefail

# Resolve project root (dois níveis acima -> repo root)
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

# Define o nome do arquivo de segredos local
ENV_FILE="./.env.dev.local"

# Prepara os argumentos do --env-file
ENV_FILE_ARG=()
if [ -f "$ENV_FILE" ]; then
  echo "Usando arquivo de segredos: $ENV_FILE"
  ENV_FILE_ARG=("--env-file" "$ENV_FILE")
  export DEV_ENV_FILE="$ENV_FILE"
else
  echo "Aviso: Arquivo '$ENV_FILE' não encontrado. Tentando derrubar sem ele..." >&2
  # Se não encontrar, tenta usar o .env.dev como fallback
  if [ -f "./.env.dev" ]; then
    echo "Usando template .env.dev como fallback..."
    ENV_FILE_ARG=("--env-file" "./.env.dev")
    export DEV_ENV_FILE="./.env.dev"
  fi
fi

COMPOSE_FILES=("-f" "docker-compose.dev.yml")
if [ -f "docker-compose.override.yml" ]; then
  COMPOSE_FILES+=("-f" "docker-compose.override.yml")
fi

docker compose "${ENV_FILE_ARG[@]}" "${COMPOSE_FILES[@]}" down --remove-orphans
