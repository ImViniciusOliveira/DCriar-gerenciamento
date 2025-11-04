#!/usr/bin/env bash
# scripts/deploys/deploy-prod.sh
# Copia um arquivo .env.prod para /etc/dcriar/.env.prod e sobe o docker compose de produção

set -euo pipefail

# Resolve project root (dois níveis acima deste script)
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

ENV_SRC="$1"
if [ -z "${ENV_SRC:-}" ]; then
  echo "Uso: $0 /caminho/para/.env.prod" >&2
  exit 2
fi

DEST_DIR="/etc/dcriar"
DEST_FILE="$DEST_DIR/.env.prod"

# Cria o diretório se não existir (requiere sudo)
if [ ! -d "$DEST_DIR" ]; then
  echo "Criando $DEST_DIR (precisa de sudo)..."
  sudo mkdir -p "$DEST_DIR"
  sudo chown "$USER":"$USER" "$DEST_DIR"
fi

# Verifica se o arquivo fonte existe
if [ ! -f "$ENV_SRC" ]; then
  echo "Arquivo de origem '$ENV_SRC' não encontrado." >&2
  exit 3
fi

# Copia o arquivo para /etc/dcriar/.env.prod (preserva permissões)
echo "Copiando $ENV_SRC para $DEST_FILE (precisa de sudo)..."
sudo cp "$ENV_SRC" "$DEST_FILE"
sudo chmod 640 "$DEST_FILE"

# Exporta variável para o docker compose
export PROD_ENV_FILE="$DEST_FILE"

# Caminho absoluto do docker-compose prod
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.prod.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
  echo "Arquivo $COMPOSE_FILE não encontrado. Execute este script a partir do repositório correto." >&2
  exit 4
fi

# (Opcional) Puxar imagens
if [ "${NO_PULL:-0}" -eq 0 ]; then
  echo "Pull das imagens (se disponíveis)..."
  docker compose -f "$COMPOSE_FILE" --env-file "$DEST_FILE" pull --ignore-pull-failures || true
fi

# Up
echo "Subindo stack de produção (detached)..."
docker compose -f "$COMPOSE_FILE" --env-file "$DEST_FILE" up -d --build

# Mostrar status
docker compose -f "$COMPOSE_FILE" --env-file "$DEST_FILE" ps

echo "Deploy finalizado. Verifique logs com: docker compose -f $COMPOSE_FILE --env-file $DEST_FILE logs -f <service>"
