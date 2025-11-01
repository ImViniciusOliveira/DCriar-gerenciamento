#!/usr/bin/env bash
# scripts/up-dev.sh
# Inicia o ambiente de desenvolvimento Docker Compose usando o arquivo de secrets apropriado.
# Uso: ./scripts/up-dev.sh [serviços-ou-opções]

set -euo pipefail

# Prioridade: variável DEV_ENV_FILE -> /home/viniciusdev/dcriar/.secrets/.env.dev -> ./.env.dev
ENV_CANDIDATE=${DEV_ENV_FILE:-}
if [ -z "${ENV_CANDIDATE}" ]; then
  if [ -f "/home/viniciusdev/dcriar/.secrets/.env.dev" ]; then
    ENV_CANDIDATE="/home/viniciusdev/dcriar/.secrets/.env.dev"
  else
    ENV_CANDIDATE="./.env.dev"
  fi
fi

if [ ! -f "$ENV_CANDIDATE" ]; then
  echo "Aviso: arquivo de variáveis não encontrado: $ENV_CANDIDATE" >&2
  echo "Crie um arquivo .env dev ou exporte DEV_ENV_FILE apontando para o arquivo correto." >&2
  exit 2
fi

# Se houver um override presente, inclua-o no comando compose
COMPOSE_FILES=("-f" "docker-compose.dev.yml")
if [ -f "docker-compose.override.yml" ]; then
  COMPOSE_FILES+=("-f" "docker-compose.override.yml")
fi

echo "Usando arquivo de env: $ENV_CANDIDATE"

docker compose --env-file "$ENV_CANDIDATE" "${COMPOSE_FILES[@]}" up --build "$@"
