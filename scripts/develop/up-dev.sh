#!/usr/bin/env bash
# scripts/develop/up-dev.sh
# Inicia o ambiente de desenvolvimento usando o arquivo de segredos ./.env.dev.local

set -euo pipefail

# Resolve project root (dois níveis acima deste script -> repo root)
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

# Suporta opção --no-backend para não incluir docker-compose.override.yml
SKIP_OVERRIDE=0
NEW_ARGS=()
for a in "$@"; do
  if [ "$a" = "--no-backend" ]; then
    SKIP_OVERRIDE=1
  else
    NEW_ARGS+=("$a")
  fi
done
# Reatribui argumentos (sem o --no-backend)
set -- "${NEW_ARGS[@]}"

# Define o nome do arquivo de segredos local
ENV_FILE="./.env.dev.local"

# Verifica se o arquivo de segredos existe
if [ ! -f "$ENV_FILE" ]; then
  echo "ERRO: Arquivo de segredos '$ENV_FILE' não encontrado." >&2
  echo "Por favor, copie o template '.env.dev' para '$ENV_FILE' e preencha seus segredos." >&2
  echo "Exemplo: cp .env.dev .env.dev.local" >&2
  exit 1
fi

# Se houver um override presente, inclua-o no comando compose (a menos que --no-backend seja usado)
COMPOSE_FILES=("-f" "docker-compose.dev.yml")
if [ "$SKIP_OVERRIDE" -eq 0 ] && [ -f "docker-compose.override.yml" ]; then
  COMPOSE_FILES+=("-f" "docker-compose.override.yml")
else
  if [ "$SKIP_OVERRIDE" -eq 1 ]; then
    echo "Nota: ignorando docker-compose.override.yml (não será iniciado o container 'backend')."
  fi
fi

echo "Usando arquivo de segredos: $ENV_FILE"

# Define a variável DEV_ENV_FILE que os arquivos YML esperam
export DEV_ENV_FILE="$ENV_FILE"

# Executa o docker compose
docker compose --env-file "$ENV_FILE" "${COMPOSE_FILES[@]}" up --build "$@"
