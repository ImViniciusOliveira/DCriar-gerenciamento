#!/usr/bin/env bash
# scripts/deploys/down-prod.sh

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

# Diminui a stack de produção e remove containers órfãos
exec ./scripts/lib/compose-run.sh --mode prod down --remove-orphans
