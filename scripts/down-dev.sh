#!/usr/bin/env bash
# scripts/down-dev.sh
# Destroi o ambiente de desenvolvimento Docker Compose e remove containers órfãos.

set -euo pipefail

# Usa o mesmo método de escolha de arquivo de env apenas por consistência (não estritamente necessário para down)
ENV_CANDIDATE=${DEV_ENV_FILE:-}
if [ -z "${ENV_CANDIDATE}" ]; then
  if [ -f "/home/viniciusdev/dcriar/.secrets/.env.dev" ]; then
    ENV_CANDIDATE="/home/viniciusdev/dcriar/.secrets/.env.dev"
  else
    ENV_CANDIDATE="./.env.dev"
  fi
fi

echo "Usando arquivo de env (opcional): $ENV_CANDIDATE"

COMPOSE_FILES=("-f" "docker-compose.dev.yml")
if [ -f "docker-compose.override.yml" ]; then
  COMPOSE_FILES+=("-f" "docker-compose.override.yml")
fi

docker compose --env-file "$ENV_CANDIDATE" "${COMPOSE_FILES[@]}" down --remove-orphans
