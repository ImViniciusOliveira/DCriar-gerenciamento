#!/usr/bin/env bash
# scripts/up-dev.sh
# Inicia o ambiente de desenvolvimento usando o arquivo de segredos ./.env.dev.local

set -euo pipefail

# Define o nome do arquivo de segredos local
ENV_FILE="./.env.dev.local"

# Verifica se o arquivo de segredos existe
if [ ! -f "$ENV_FILE" ]; then
  echo "ERRO: Arquivo de segredos '$ENV_FILE' não encontrado." >&2
  echo "Por favor, copie o template '.env.dev' para '$ENV_FILE' e preencha seus segredos." >&2
  echo "Exemplo: cp .env.dev .env.dev.local" >&2
  exit 1
fi

# Se houver um override presente, inclua-o no comando compose
COMPOSE_FILES=("-f" "docker-compose.dev.yml")
if [ -f "docker-compose.override.yml" ]; then
  COMPOSE_FILES+=("-f" "docker-compose.override.yml")
fi

echo "Usando arquivo de segredos: $ENV_FILE"

# Define a variável DEV_ENV_FILE que os arquivos YML esperam
export DEV_ENV_FILE="$ENV_FILE"

docker compose --env-file "$ENV_FILE" "${COMPOSE_FILES[@]}" up --build "$@"
