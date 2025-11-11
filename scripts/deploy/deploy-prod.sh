#!/usr/bin/env bash
set -euo pipefail

# scripts/deploy/deploy-prod.sh
# Instala o .env de produção e o docker-compose em /opt, e sobe a stack de produção.
# Uso: sudo ./scripts/deploy/deploy-prod.sh /caminho/para/.env.prod

ENV_SRC="$1"
if [ -z "${ENV_SRC:-}" ]; then
  echo "Uso: $0 /caminho/para/.env.prod" >&2
  exit 2
fi

if [ ! -f "$ENV_SRC" ]; then
  echo "Arquivo de origem '$ENV_SRC' não encontrado." >&2
  exit 3
fi

# 1) instalar env e compose (ajusta permissões)
./scripts/deploy/install-prod-env.sh "$ENV_SRC"
./scripts/deploy/install-prod-compose.sh ./docker-compose.prod.yml

echo "Fazendo pull das imagens (se disponíveis)..."
./scripts/lib/compose-run.sh --mode prod pull --ignore-pull-failures || true

echo "Subindo stack de produção (detached, com --pull)..."
./scripts/lib/compose-run.sh --mode prod up -d --pull always

echo "Deploy concluído. Verifique status com:"
echo "  ./scripts/lib/compose-run.sh --mode prod ps"
echo "Logs: ./scripts/lib/compose-run.sh --mode prod logs backend"
