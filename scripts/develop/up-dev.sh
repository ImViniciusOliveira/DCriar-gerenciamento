#!/usr/bin/env bash

# scripts/develop/up-dev.sh
# Sobe a stack de desenvolvimento. Use --no-backend para não iniciar o serviço 'backend' definido no override.

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

# Parse simples: aceita --no-backend (remove docker-compose.override.yml do comando)
NO_BACKEND=false
ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-backend)
      NO_BACKEND=true; shift;;
    --no-backend=*)
      NO_BACKEND=true; shift;;
    -h|--help)
      echo "Uso: $0 [--no-backend] [args para docker compose up]"; exit 0;;
    *) ARGS+=("$1"); shift;;
  esac
done

if [ "$NO_BACKEND" = true ]; then
  # executa apenas com o compose principal (ignora override que geralmente inicia o backend)
  exec ./scripts/lib/compose-run.sh --mode dev --compose-file docker-compose.dev.yml up "${ARGS[@]}"
else
  exec ./scripts/lib/compose-run.sh --mode dev up "${ARGS[@]}"
fi
