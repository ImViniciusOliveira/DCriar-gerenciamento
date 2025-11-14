#!/usr/bin/env bash
# scripts/develop/down-dev.sh

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

PROJECT_NAME="dcriar-dev"
ENV_FILE="./.env.dev.local"
COMPOSE_FILE_BASE="docker-compose.dev.yml"
COMPOSE_FILE_OVERRIDE="docker-compose.override.yml"
COMPOSE_RUN_CMD="./scripts/lib/compose-run.sh"

echo "INFO: Derrubando ambiente DEV..."
# Usamos 'docker compose -f ... down' diretamente para ser independente do nome do projeto.
# Isso torna o script mais robusto contra "contêineres órfãos" iniciados sem --project-name.
exec docker compose \
  --project-name "$PROJECT_NAME" \
  ${ENV_FILE:+--env-file "$ENV_FILE"} \
  -f "$COMPOSE_FILE_BASE" \
  -f "$COMPOSE_FILE_OVERRIDE" \
  down --remove-orphans
