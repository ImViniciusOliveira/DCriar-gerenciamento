#!/usr/bin/env bash
set -euo pipefail

# install-prod-env.sh
# Copia um arquivo .env.prod do repositório (ou caminho informado) para /etc/dcriar/.env.prod
# e ajusta permissões para produção.
# Uso: sudo ./scripts/deploy/install-prod-env.sh [caminho_para_.env.prod]

SRC_FILE="${1:-./.env.prod}"
DEST_DIR="/etc/dcriar"
DEST_FILE="$DEST_DIR/.env.prod"

if [ ! -f "$SRC_FILE" ]; then
  echo "Arquivo de origem '$SRC_FILE' não encontrado. Rode este script a partir da raiz do repositório ou informe o caminho." >&2
  exit 2
fi

sudo mkdir -p "$DEST_DIR"
sudo cp "$SRC_FILE" "$DEST_FILE"
sudo chown root:root "$DEST_FILE"
sudo chmod 600 "$DEST_FILE"

echo "Arquivo instalado com sucesso em $DEST_FILE e protegido com permissão 600."
