#!/usr/bin/env bash

# scripts/develop/up-dev.sh
# Sobe a stack de desenvolvimento. Use --no-backend para não iniciar o serviço 'backend' definido no override.

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

PROJECT_NAME="dcriar-dev"
ENV_FILE="./.env.dev.local"
COMPOSE_FILE_BASE="docker-compose.dev.yml"
COMPOSE_FILE_OVERRIDE="docker-compose.override.yml"
COMPOSE_RUN_CMD="./scripts/lib/compose-run.sh"

NO_BACKEND=false
ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-backend|--no-backend=*) NO_BACKEND=true; shift ;;
    -h|--help)
      echo "Uso: $0 [--no-backend] [args extras para docker compose up]"; exit 0 ;;
    *) ARGS+=("$1"); shift ;;
  esac
done

# Validações simples
if [ ! -f "$COMPOSE_FILE_BASE" ]; then echo "Erro: $COMPOSE_FILE_BASE não encontrado" >&2; exit 2; fi
if [ ! -f "$ENV_FILE" ]; then echo "Aviso: $ENV_FILE não existe; usando sem --env-file. Algumas variáveis podem ficar vazias." >&2; fi

if [ "$NO_BACKEND" = true ]; then
  echo "INFO: Subindo DEV sem backend..."
  exec "$COMPOSE_RUN_CMD" \
    --no-sudo \
    --project-name "$PROJECT_NAME" \
    ${ENV_FILE:+--env-file "$ENV_FILE"} \
    --compose-file "$COMPOSE_FILE_BASE" \
    up -d "${ARGS[@]}"
else
  echo "INFO: Subindo DEV completo..."
  exec "$COMPOSE_RUN_CMD" \
    --no-sudo \
    --project-name "$PROJECT_NAME" \
    ${ENV_FILE:+--env-file "$ENV_FILE"} \
    --compose-file "$COMPOSE_FILE_BASE" \
    --compose-file "$COMPOSE_FILE_OVERRIDE" \
    up -d "${ARGS[@]}"
fi
