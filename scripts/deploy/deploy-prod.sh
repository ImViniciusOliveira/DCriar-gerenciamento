#!/usr/bin/env bash
set -euo pipefail

# scripts/deploy/deploy-prod.sh
# Wrapper simples para instalar .env.prod e docker-compose.prod.yml e subir a stack.
# Uso: sudo ./scripts/deploy/deploy-prod.sh /caminho/para/.env.prod

ENV_SRC="$1"
if [ -z "${ENV_SRC:-}" ]; then
  echo "Uso: $0 /caminho/para/.env.prod" >&2
  exit 2
fi

# Verifica arquivos
if [ ! -f "$ENV_SRC" ]; then
  echo "Arquivo de origem '$ENV_SRC' não encontrado." >&2
  exit 3
fi

# 1) instalar env e compose (usa os helpers que ajustam permissões corretamente)
# install-prod-env.sh e install-prod-compose.sh usam sudo internamente quando necessário
./scripts/deploy/install-prod-env.sh "$ENV_SRC"
./scripts/deploy/install-prod-compose.sh ./docker-compose.prod.yml

# 2) definir variável e subir
export PROD_ENV_FILE=/etc/dcriar/.env.prod

echo "Fazendo pull das imagens (se disponíveis)..."
PROD_ENV_FILE=$PROD_ENV_FILE docker compose -f /opt/dcriar/docker-compose.prod.yml pull --ignore-pull-failures || true

echo "Subindo stack de produção (detached, com --pull)..."
PROD_ENV_FILE=$PROD_ENV_FILE docker compose -f /opt/dcriar/docker-compose.prod.yml up -d --pull always

echo "Deploy concluído. Verifique status com:"
echo "  PROD_ENV_FILE=$PROD_ENV_FILE docker compose -f /opt/dcriar/docker-compose.prod.yml ps"
echo "Logs: PROD_ENV_FILE=$PROD_ENV_FILE docker compose -f /opt/dcriar/docker-compose.prod.yml logs -f backend"
