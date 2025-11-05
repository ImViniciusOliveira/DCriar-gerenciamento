#!/usr/bin/env bash
# scripts/deploys/down-prod.sh
# Para parar e remover containers e rede do ambiente de produção (usa /etc/dcriar/.env.prod)

set -euo pipefail

ENV_FILE="/etc/dcriar/.env.prod"
COMPOSE_FILE="/opt/dcriar/docker-compose.prod.yml"

if [ ! -f "$ENV_FILE" ]; then
  echo "Arquivo $ENV_FILE não encontrado. Forneça o arquivo de produção em /etc/dcriar/.env.prod" >&2
  exit 1
fi

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "Arquivo $COMPOSE_FILE não encontrado. Execute este script a partir do repositório correto ou instale o compose em /opt/dcriar." >&2
  exit 2
fi

export PROD_ENV_FILE="$ENV_FILE"

echo "Derrubando stack de produção..."
PROD_ENV_FILE=$PROD_ENV_FILE docker compose -f $COMPOSE_FILE --env-file $ENV_FILE down --remove-orphans

echo "Serviços de produção parados e containers removidos."
