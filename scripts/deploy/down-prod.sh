#!/usr/bin/env bash
# scripts/deploy/down-prod.sh

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

PROD_ENV_FILE="/etc/dcriar/.env.prod"
PROD_COMPOSE_FILE="/opt/dcriar/docker-compose.prod.yml"
PROJECT_NAME="dcriar-prod"
COMPOSE_RUN_CMD="./scripts/lib/compose-run.sh"

echo "INFO: Derrubando stack de PRODUÇÃO (local-teste)..."
exec sudo "$COMPOSE_RUN_CMD" \
  --project-name "$PROJECT_NAME" \
  --env-file "$PROD_ENV_FILE" \
  --compose-file "$PROD_COMPOSE_FILE" \
  down --remove-orphans
