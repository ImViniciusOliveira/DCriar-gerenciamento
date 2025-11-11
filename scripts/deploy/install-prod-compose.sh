#!/usr/bin/env bash
set -euo pipefail

# install-prod-compose.sh
# Copia o docker-compose.prod.yml para /opt/dcriar/docker-compose.prod.yml e ajusta permissões
# Uso: sudo ./scripts/deploy/install-prod-compose.sh [caminho_para_docker-compose.prod.yml]

SRC_FILE="${1:-./docker-compose.prod.yml}"
DEST_DIR="/opt/dcriar"
DEST_FILE="$DEST_DIR/docker-compose.prod.yml"

if [ ! -f "$SRC_FILE" ]; then
  echo "Arquivo de origem '$SRC_FILE' não encontrado. Rode este script a partir da raiz do repositório ou informe o caminho." >&2
  exit 2
fi

sudo mkdir -p "$DEST_DIR"
sudo cp "$SRC_FILE" "$DEST_FILE"
sudo chown root:root "$DEST_FILE"
sudo chmod 644 "$DEST_FILE"

echo "Arquivo docker-compose copiado para $DEST_FILE (perm: 644)."
